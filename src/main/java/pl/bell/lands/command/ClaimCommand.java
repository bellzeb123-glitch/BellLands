package pl.bell.lands.command;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.integration.Pl3xMapHook;

import java.util.Optional;
import java.util.stream.Collectors;

public class ClaimCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cKomenda tylko dla graczy.");
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();

        if (args.length == 0) {
            // Zajmowanie dzialki
            if (!player.hasPermission("belllands.claim") && !player.isOp()) {
                player.sendMessage("§cNie masz uprawnien do zajmowania terenu!");
                return true;
            }
            if (landManager.isClaimed(chunk)) {
                player.sendMessage("§cTen teren jest juz zajety!");
                return true;
            }

            Land land = new Land(player.getUniqueId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
            landManager.claimLand(land);

            // Wyrysowanie na mapie
            Pl3xMapHook.drawLand(land);

            player.sendMessage("§aZajales teren! Chunk: §f" + chunk.getX() + "§7, §f" + chunk.getZ());
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "unclaim" -> handleUnclaim(player, chunk, landManager);
            case "trust" -> handleTrust(player, args, chunk, landManager);
            case "untrust" -> handleUntrust(player, args, chunk, landManager);
            case "flag" -> handleFlag(player, args, chunk, landManager);
            case "flags" -> handleFlagsList(player, chunk, landManager);
            case "info" -> handleInfo(player, chunk, landManager);
            case "help" -> sendHelp(player);
            default -> sendHelp(player);
        }
        return true;
    }

    // ========================================================
    //  UNCLAIM
    // ========================================================

    private void handleUnclaim(Player player, Chunk chunk, LandManager landManager) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage("§cTen teren nie jest zajety przez nikogo.");
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage("§cTylko wlasciciel moze usunac ten claim!");
            return;
        }
        landManager.unclaimLand(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        Pl3xMapHook.removeLand(land);
        player.sendMessage("§aUsunales claim z tego terenu.");
    }

    // ========================================================
    //  TRUST
    // ========================================================

    private void handleTrust(Player player, String[] args, Chunk chunk, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage("§cUzycie: §e/claim trust <gracz>");
            return;
        }
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage("§cTen teren nie nalezy do nikogo.");
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage("§cTylko wlasciciel moze zarzadzac zaufanymi!");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (land.isTrusted(target.getUniqueId())) {
            player.sendMessage("§eTen gracz jest juz zaufany na tej dzialce.");
            return;
        }
        land.addTrusted(target.getUniqueId());
        landManager.saveLand(land);
        player.sendMessage("§aDodano gracza §f" + target.getName() + " §ado zaufanych na tej dzialce.");
    }

    // ========================================================
    //  UNTRUST
    // ========================================================

    private void handleUntrust(Player player, String[] args, Chunk chunk, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage("§cUzycie: §e/claim untrust <gracz>");
            return;
        }
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage("§cTen teren nie nalezy do nikogo.");
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage("§cTylko wlasciciel moze zarzadzac zaufanymi!");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!land.isTrusted(target.getUniqueId())) {
            player.sendMessage("§cTen gracz nie jest zaufany na tej dzialce.");
            return;
        }
        land.removeTrusted(target.getUniqueId());
        landManager.saveLand(land);
        player.sendMessage("§aUsunieto gracza §f" + target.getName() + " §az zaufanych na tej dzialce.");
    }

    // ========================================================
    //  FLAG (ustawianie pojedynczej flagi)
    // ========================================================

    private void handleFlag(Player player, String[] args, Chunk chunk, LandManager landManager) {
        if (args.length < 2) {
            player.sendMessage("§cUzycie: §e/claim flag <flaga> <true|false>");
            player.sendMessage("§7Dostepne flagi: §f" + String.join(", ", Land.ALL_FLAGS));
            return;
        }
        if (args.length < 3) {
            // Pokaz wartosc danej flagi
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isEmpty()) {
                player.sendMessage("§cTen teren nie nalezy do nikogo.");
                return;
            }
            String flag = args[1].toLowerCase();
            if (!Land.isValidFlag(flag)) {
                player.sendMessage("§cNieznana flaga: §e" + flag);
                player.sendMessage("§7Dostepne flagi: §f" + String.join(", ", Land.ALL_FLAGS));
                return;
            }
            Land land = opt.get();
            boolean value = land.getFlag(flag);
            player.sendMessage("§7Flaga §f" + flag + "§7: " + (value ? "§awlaczona" : "§cwylaczona"));
            return;
        }

        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage("§cTen teren nie nalezy do nikogo.");
            return;
        }
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) {
            player.sendMessage("§cTylko wlasciciel moze zmieniac flagi!");
            return;
        }

        String flag = args[1].toLowerCase();
        if (!Land.isValidFlag(flag)) {
            player.sendMessage("§cNieznana flaga: §e" + flag);
            player.sendMessage("§7Dostepne flagi: §f" + String.join(", ", Land.ALL_FLAGS));
            return;
        }

        String rawValue = args[2].toLowerCase();
        if (!rawValue.equals("true") && !rawValue.equals("false")) {
            player.sendMessage("§cWartosc musi byc §etrue §club §efalse§c.");
            return;
        }

        boolean value = Boolean.parseBoolean(rawValue);
        land.setFlag(flag, value);
        landManager.saveLand(land);
        player.sendMessage("§aFlaga §f" + flag + " §azostala ustawiona na: " + (value ? "§awlaczona" : "§cwylaczona"));
    }

    // ========================================================
    //  FLAGS (lista wszystkich flag)
    // ========================================================

    private void handleFlagsList(Player player, Chunk chunk, LandManager landManager) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage("§eTen teren jest wolny.");
            return;
        }
        Land land = opt.get();
        player.sendMessage("§6=== Flagi dzialki ===");
        for (String flag : Land.ALL_FLAGS) {
            boolean value = land.getFlag(flag);
            String status = value ? "§a✔ wlaczona" : "§c✘ wylaczona";
            player.sendMessage("§7  " + flag + ": " + status);
        }
        player.sendMessage("§6=====================");
        player.sendMessage("§7Zmien flage: §e/claim flag <nazwa> <true|false>");
    }

    // ========================================================
    //  INFO
    // ========================================================

    private void handleInfo(Player player, Chunk chunk, LandManager landManager) {
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) {
            player.sendMessage("§eTen teren jest wolny.");
            return;
        }
        Land land = opt.get();
        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = "Nieznany";

        String trustedNames = land.getTrusted().stream()
            .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
            .filter(name -> name != null)
            .collect(Collectors.joining(", "));
        if (trustedNames.isEmpty()) trustedNames = "Brak";

        player.sendMessage("§6=== Informacje o Dzialce ===");
        player.sendMessage("§7Wlasciciel: §f" + ownerName);
        player.sendMessage("§7Chunk: §f" + land.getChunkX() + "§7, §f" + land.getChunkZ());
        player.sendMessage("§7Swiat: §f" + land.getWorldName());
        player.sendMessage("§7Zaufani: §f" + trustedNames);
        player.sendMessage("§7");

        // Wypisz kluczowe flagi
        player.sendMessage("§7PVP: " + formatFlag(land, "pvp"));
        player.sendMessage("§7Wybuchy: " + formatFlag(land, "explosions"));
        player.sendMessage("§7Ogien: " + formatFlag(land, "fire-spread"));
        player.sendMessage("§7Moby (spawn): " + formatFlag(land, "mob-spawning"));
        player.sendMessage("§7Moby (obrazenia): " + formatFlag(land, "mob-damage"));
        player.sendMessage("§7Lava: " + formatFlag(land, "lava-flow"));
        player.sendMessage("§7Woda: " + formatFlag(land, "water-flow"));
        player.sendMessage("§7Tloki: " + formatFlag(land, "piston"));
        player.sendMessage("§7Rozpad lisci: " + formatFlag(land, "leaf-decay"));
        player.sendMessage("§7Interakcja (use): " + formatFlag(land, "use"));
        player.sendMessage("§6============================");
    }

    private String formatFlag(Land land, String flag) {
        return land.getFlag(flag) ? "§awlaczone" : "§cwylaczone";
    }

    // ========================================================
    //  HELP
    // ========================================================

    private void sendHelp(Player player) {
        player.sendMessage("§6=== Komendy /claim ===");
        player.sendMessage("§e/claim §7- Zajmuje chunk, na ktorym stoisz");
        player.sendMessage("§e/claim unclaim §7- Usuwa claim");
        player.sendMessage("§e/claim info §7- Informacje o dzialce");
        player.sendMessage("§e/claim flags §7- Lista wszystkich flag");
        player.sendMessage("§e/claim flag <flaga> <true|false> §7- Zmienia flage");
        player.sendMessage("§e/claim trust <gracz> §7- Dodaje zaufanego gracza");
        player.sendMessage("§e/claim untrust <gracz> §7- Usuwa zaufanego gracza");
        player.sendMessage("§e/claim help §7- Ta pomoc");
        player.sendMessage("§6======================");
    }
}