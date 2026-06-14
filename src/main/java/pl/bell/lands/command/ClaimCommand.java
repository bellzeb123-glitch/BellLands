package pl.bell.lands.command;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.gui.ClaimGui;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.integration.Pl3xMapHook;

import java.util.Optional;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = BellLands.getInstance().getLangManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lang.component("only-players"));
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();

        if (args.length == 0) {
            // If standing on own land, open GUI; otherwise claim
            Optional<Land> existing = landManager.getLandAt(chunk);
            if (existing.isPresent() && existing.get().getOwner().equals(player.getUniqueId())) {
                ClaimGui.openMain(player, existing.get());
                return true;
            }

            if (!player.hasPermission("belllands.claim") && !player.isOp()) {
                player.sendMessage(lang.component("no-permission"));
                return true;
            }
            if (landManager.isClaimed(chunk)) {
                player.sendMessage(lang.component("claim-already-claimed"));
                return true;
            }

            int current = landManager.getClaimCount(player.getUniqueId());
            int max = landManager.getMaxClaims(player);
            if (current >= max) {
                player.sendMessage(lang.component("claim-limit-reached",
                    "current", current, "max", max));
                return true;
            }

            Land land = new Land(player.getUniqueId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            landManager.claimLand(land);
            Pl3xMapHook.drawLand(land);

            player.sendMessage(lang.component("claim-success",
                "x", chunk.getX(), "z", chunk.getZ()));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "unclaim" -> handleUnclaim(player, chunk, landManager, lang);
            case "trust" -> handleTrust(player, args, chunk, landManager, lang);
            case "untrust" -> handleUntrust(player, args, chunk, landManager, lang);
            case "flag" -> handleFlag(player, args, chunk, landManager, lang);
            case "flags" -> handleFlagsList(player, chunk, landManager, lang);
            case "info" -> handleInfo(player, chunk, landManager, lang);
            case "menu" -> handleGui(player, chunk, landManager, lang);
            case "help" -> sendHelp(player, lang);
            default -> sendHelp(player, lang);
        }
        return true;
    }

    private void handleGui(Player player, Chunk chunk, LandManager landManager, LangManager lang) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("info-not-claimed"));
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(lang.component("unclaim-not-owner"));
            return;
        }
        ClaimGui.openMain(player, land);
    }

    private void handleUnclaim(Player player, Chunk chunk, LandManager landManager, LangManager lang) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("unclaim-not-claimed"));
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(lang.component("unclaim-not-owner"));
            return;
        }
        landManager.unclaimLand(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Pl3xMapHook.removeLand(land);
        player.sendMessage(lang.component("unclaim-success"));
    }

    private void handleTrust(Player player, String[] args, Chunk chunk, LandManager landManager, LangManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.component("trust-usage"));
            return;
        }
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("trust-not-claimed"));
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(lang.component("trust-not-owner"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (land.isTrusted(target.getUniqueId())) {
            player.sendMessage(lang.component("trust-already"));
            return;
        }
        land.addTrusted(target.getUniqueId());
        landManager.saveLand(land);
        player.sendMessage(lang.component("trust-success", "player", target.getName()));
    }

    private void handleUntrust(Player player, String[] args, Chunk chunk, LandManager landManager, LangManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.component("untrust-usage"));
            return;
        }
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("untrust-not-claimed"));
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(lang.component("untrust-not-owner"));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!land.isTrusted(target.getUniqueId())) {
            player.sendMessage(lang.component("untrust-not-trusted"));
            return;
        }
        land.removeTrusted(target.getUniqueId());
        landManager.saveLand(land);
        player.sendMessage(lang.component("untrust-success", "player", target.getName()));
    }

    private void handleFlag(Player player, String[] args, Chunk chunk, LandManager landManager, LangManager lang) {
        if (args.length < 2) {
            player.sendMessage(lang.component("flag-usage"));
            player.sendMessage(lang.component("flag-available",
                "flags", String.join(", ", Land.ALL_FLAGS)));
            return;
        }
        if (args.length < 3) {
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isEmpty()) {
                player.sendMessage(lang.component("flag-not-claimed"));
                return;
            }
            String flag = args[1].toLowerCase();
            if (!Land.isValidFlag(flag)) {
                player.sendMessage(lang.component("flag-unknown", "flag", flag));
                player.sendMessage(lang.component("flag-available",
                    "flags", String.join(", ", Land.ALL_FLAGS)));
                return;
            }
            Land land = opt.get();
            boolean value = land.getFlag(flag);
            String valueStr = value ? lang.getRaw("flag-enabled") : lang.getRaw("flag-disabled");
            player.sendMessage(lang.component("flag-value", "flag", flag, "value", valueStr));
            return;
        }

        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("flag-not-claimed"));
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage(lang.component("flag-not-owner"));
            return;
        }

        String flag = args[1].toLowerCase();
        if (!Land.isValidFlag(flag)) {
            player.sendMessage(lang.component("flag-unknown", "flag", flag));
            player.sendMessage(lang.component("flag-available",
                "flags", String.join(", ", Land.ALL_FLAGS)));
            return;
        }

        String rawValue = args[2].toLowerCase();
        if (!rawValue.equals("true") && !rawValue.equals("false")) {
            player.sendMessage(lang.component("flag-invalid-value"));
            return;
        }

        boolean value = Boolean.parseBoolean(rawValue);
        land.setFlag(flag, value);
        landManager.saveLand(land);
        String valueStr = value ? lang.getRaw("flag-enabled") : lang.getRaw("flag-disabled");
        player.sendMessage(lang.component("flag-set", "flag", flag, "value", valueStr));
    }

    private void handleFlagsList(Player player, Chunk chunk, LandManager landManager, LangManager lang) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("flags-not-claimed"));
            return;
        }
        Land land = opt.get();
        player.sendMessage(lang.componentRaw("flags-header"));
        for (String flag : Land.ALL_FLAGS) {
            boolean value = land.getFlag(flag);
            String key = value ? "flags-entry-on" : "flags-entry-off";
            player.sendMessage(lang.componentRaw(key, "flag", flag));
        }
        player.sendMessage(lang.componentRaw("flags-footer"));
        player.sendMessage(lang.componentRaw("flags-hint"));
    }

    private void handleInfo(Player player, Chunk chunk, LandManager landManager, LangManager lang) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage(lang.component("info-not-claimed"));
            return;
        }
        Land land = opt.get();
        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = lang.getRaw("info-unknown-owner");

        String trustedNames = land.getTrusted().stream()
            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
            .filter(name -> name != null)
            .collect(Collectors.joining(", "));
        if (trustedNames.isEmpty()) trustedNames = lang.getRaw("info-trusted-none");

        player.sendMessage(lang.componentRaw("info-header"));
        player.sendMessage(lang.componentRaw("info-owner", "owner", ownerName));
        player.sendMessage(lang.componentRaw("info-chunk", "x", land.getChunkX(), "z", land.getChunkZ()));
        player.sendMessage(lang.componentRaw("info-world", "world", land.getWorldName()));
        player.sendMessage(lang.componentRaw("info-trusted", "trusted", trustedNames));
        player.sendMessage(lang.componentRaw(""));

        String on = lang.getRaw("info-flag-on");
        String off = lang.getRaw("info-flag-off");
        for (String flag : Land.ALL_FLAGS) {
            String val = land.getFlag(flag) ? on : off;
            player.sendMessage(lang.componentRaw("info-flag-" + flag, "value", val));
        }
        player.sendMessage(lang.componentRaw("info-footer"));
    }

    private void sendHelp(Player player, LangManager lang) {
        player.sendMessage(lang.componentRaw("help-header"));
        player.sendMessage(lang.componentRaw("help-claim"));
        player.sendMessage(lang.componentRaw("help-unclaim"));
        player.sendMessage(lang.componentRaw("help-info"));
        player.sendMessage(lang.componentRaw("help-flags"));
        player.sendMessage(lang.componentRaw("help-flag"));
        player.sendMessage(lang.componentRaw("help-trust"));
        player.sendMessage(lang.componentRaw("help-untrust"));
        player.sendMessage(lang.componentRaw("help-menu"));
        player.sendMessage(lang.componentRaw("help-help"));
        player.sendMessage(lang.componentRaw("help-footer"));
    }
}
