package pl.bell.lands.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class WarpManager {

    private final Map<UUID, Map<String, Location>> warps = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public void init() {
        File folder = BellLands.getInstance().getDataFolder();
        this.file = new File(folder, "warps.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public boolean setWarp(UUID owner, String name, Location location) {
        warps.computeIfAbsent(owner, k -> new LinkedHashMap<>()).put(name.toLowerCase(), location);
        save(owner);
        return true;
    }

    public boolean deleteWarp(UUID owner, String name) {
        Map<String, Location> playerWarps = warps.get(owner);
        if (playerWarps == null) return false;
        boolean removed = playerWarps.remove(name.toLowerCase()) != null;
        if (removed) save(owner);
        return removed;
    }

    public Location getWarp(UUID owner, String name) {
        Map<String, Location> playerWarps = warps.get(owner);
        if (playerWarps == null) return null;
        return playerWarps.get(name.toLowerCase());
    }

    public Set<String> getWarpNames(UUID owner) {
        Map<String, Location> playerWarps = warps.get(owner);
        if (playerWarps == null) return Set.of();
        return playerWarps.keySet();
    }

    public int getWarpCount(UUID owner) {
        Map<String, Location> playerWarps = warps.get(owner);
        return playerWarps == null ? 0 : playerWarps.size();
    }

    public int getMaxWarps(Player player) {
        int configMax = BellLands.getInstance().getConfig().getInt("claims.max-warps", 3);
        for (int i = 50; i >= 1; i--) {
            if (player.hasPermission("belllands.warps." + i)) {
                return i;
            }
        }
        return configMax;
    }

    private void save(UUID owner) {
        Map<String, Location> playerWarps = warps.get(owner);
        String ownerKey = owner.toString();
        if (playerWarps == null || playerWarps.isEmpty()) {
            config.set("warps." + ownerKey, null);
        } else {
            for (Map.Entry<String, Location> entry : playerWarps.entrySet()) {
                String path = "warps." + ownerKey + "." + entry.getKey();
                Location loc = entry.getValue();
                config.set(path + ".world", loc.getWorld().getName());
                config.set(path + ".x", loc.getX());
                config.set(path + ".y", loc.getY());
                config.set(path + ".z", loc.getZ());
                config.set(path + ".yaw", (double) loc.getYaw());
                config.set(path + ".pitch", (double) loc.getPitch());
            }
        }
        saveFile();
    }

    private void loadAll() {
        warps.clear();
        var section = config.getConfigurationSection("warps");
        if (section == null) return;

        for (String ownerKey : section.getKeys(false)) {
            UUID owner;
            try {
                owner = UUID.fromString(ownerKey);
            } catch (IllegalArgumentException e) {
                continue;
            }

            var warpSection = section.getConfigurationSection(ownerKey);
            if (warpSection == null) continue;

            Map<String, Location> playerWarps = new LinkedHashMap<>();
            for (String warpName : warpSection.getKeys(false)) {
                String path = ownerKey + "." + warpName;
                String worldName = section.getString(path + ".world");
                if (worldName == null) continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;

                double x = section.getDouble(path + ".x");
                double y = section.getDouble(path + ".y");
                double z = section.getDouble(path + ".z");
                float yaw = (float) section.getDouble(path + ".yaw");
                float pitch = (float) section.getDouble(path + ".pitch");

                playerWarps.put(warpName, new Location(world, x, y, z, yaw, pitch));
            }

            if (!playerWarps.isEmpty()) {
                warps.put(owner, playerWarps);
            }
        }
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to save warps.yml", e);
        }
    }
}
