package pl.bell.lands.command;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.WarpManager;
import pl.bell.lands.model.Land;

import java.util.Optional;
import java.util.Set;

/** Shared warp logic for /claim warp … and standalone /warp commands. */
public final class WarpCommands {

    private WarpCommands() {}

    public static boolean setWarp(Player player, String name) {
        LangManager lang = BellLands.getInstance().getLangManager();
        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty() || !opt.get().getOwner().equals(player.getUniqueId())) {
            player.sendMessage(lang.component("warp-must-own"));
            return true;
        }

        WarpManager warpManager = BellLands.getInstance().getWarpManager();
        int current = warpManager.getWarpCount(player.getUniqueId());
        int max = warpManager.getMaxWarps(player);
        String warpName = name.toLowerCase();
        boolean isUpdate = warpManager.getWarp(player.getUniqueId(), warpName) != null;

        if (!isUpdate && current >= max) {
            player.sendMessage(lang.component("warp-limit-reached", "current", current, "max", max));
            return true;
        }

        warpManager.setWarp(player.getUniqueId(), warpName, player.getLocation());
        player.sendMessage(lang.component("warp-set-success", "name", warpName));
        return true;
    }

    public static boolean deleteWarp(Player player, String name) {
        LangManager lang = BellLands.getInstance().getLangManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();
        String warpName = name.toLowerCase();
        if (warpManager.deleteWarp(player.getUniqueId(), warpName)) {
            player.sendMessage(lang.component("warp-deleted", "name", warpName));
        } else {
            player.sendMessage(lang.component("warp-not-found", "name", warpName));
        }
        return true;
    }

    public static boolean warp(Player player, String name) {
        LangManager lang = BellLands.getInstance().getLangManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();
        String warpName = name.toLowerCase();
        Location loc = warpManager.getWarp(player.getUniqueId(), warpName);
        if (loc == null) {
            player.sendMessage(lang.component("warp-not-found", "name", warpName));
            return true;
        }
        player.teleport(loc);
        player.sendMessage(lang.component("warp-teleported", "name", warpName));
        return true;
    }

    public static boolean listWarps(Player player) {
        LangManager lang = BellLands.getInstance().getLangManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();
        Set<String> names = warpManager.getWarpNames(player.getUniqueId());
        if (names.isEmpty()) {
            player.sendMessage(lang.component("warp-none"));
            return true;
        }
        int max = warpManager.getMaxWarps(player);
        player.sendMessage(lang.componentRaw("warp-list-header", "count", names.size(), "max", max));
        for (String entry : names) {
            player.sendMessage(lang.componentRaw("warp-list-entry", "name", entry));
        }
        return true;
    }
}
