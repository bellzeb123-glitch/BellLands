package pl.bell.lands.manager;

import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LandManager {

    private final Map<String, Land> claimedLands = new HashMap<>();
    private final Set<UUID> autoClaimPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> autoUnclaimPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, int[]> outlineCorner1 = new HashMap<>();
    private final Map<UUID, int[]> outlineCorner2 = new HashMap<>();
    private final Set<UUID> outlinePlayers = ConcurrentHashMap.newKeySet();
    private File file;
    private FileConfiguration config;
    private File limitsFile;
    private FileConfiguration limitsConfig;

    public void init() {
        File folder = BellLands.getInstance().getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.file = new File(folder, "lands.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.limitsFile = new File(folder, "claim-limits.yml");
        this.limitsConfig = YamlConfiguration.loadConfiguration(limitsFile);
        loadAll();
    }

    public Collection<Land> getAllLands() {
        return claimedLands.values();
    }

    public void claimLand(Land land) {
        String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        claimedLands.put(key, land);
        saveSingleLand(key, land);
    }

    public void saveLand(Land land) {
        String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        saveSingleLand(key, land);
    }

    public void unclaimLand(String world, int x, int z) {
        String key = generateKey(world, x, z);
        claimedLands.remove(key);
        config.set("claims." + key, null);
        saveFile();
    }

    public Optional<Land> getLandAt(Chunk chunk) {
        String key = generateKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        return Optional.ofNullable(claimedLands.get(key));
    }

    public boolean isClaimed(Chunk chunk) {
        return getLandAt(chunk).isPresent();
    }

    public int getClaimCount(UUID owner) {
        int count = 0;
        for (Land land : claimedLands.values()) {
            if (land.getOwner().equals(owner)) count++;
        }
        return count;
    }

    public int getMaxClaims(org.bukkit.entity.Player player) {
        int override = getClaimLimitOverride(player.getUniqueId());
        if (override > 0) return override;

        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("belllands.claims." + i)) {
                return i;
            }
        }
        return BellLands.getInstance().getConfig().getInt("claims.max-per-player", 5);
    }

    public int getClaimLimitOverride(UUID owner) {
        return limitsConfig.getInt(owner.toString(), -1);
    }

    public void setClaimLimitOverride(UUID owner, int limit) {
        if (limit <= 0) {
            limitsConfig.set(owner.toString(), null);
        } else {
            limitsConfig.set(owner.toString(), limit);
        }
        saveLimitsFile();
    }

    private void saveLimitsFile() {
        try {
            limitsConfig.save(limitsFile);
        } catch (IOException e) {
            BellLands.getInstance().getLogger().severe("Failed to save claim-limits.yml: " + e.getMessage());
        }
    }

    public boolean toggleAutoClaim(UUID player) {
        if (autoClaimPlayers.contains(player)) {
            autoClaimPlayers.remove(player);
            return false;
        }
        autoUnclaimPlayers.remove(player);
        autoClaimPlayers.add(player);
        return true;
    }

    public boolean toggleAutoUnclaim(UUID player) {
        if (autoUnclaimPlayers.contains(player)) {
            autoUnclaimPlayers.remove(player);
            return false;
        }
        autoClaimPlayers.remove(player);
        autoUnclaimPlayers.add(player);
        return true;
    }

    public boolean isAutoClaiming(UUID player) { return autoClaimPlayers.contains(player); }
    public boolean isAutoUnclaiming(UUID player) { return autoUnclaimPlayers.contains(player); }

    public void removeAutoModes(UUID player) {
        autoClaimPlayers.remove(player);
        autoUnclaimPlayers.remove(player);
        outlinePlayers.remove(player);
    }

    // ── Outline mode ────────────────────────────────────────
    public boolean toggleOutline(UUID player) {
        if (outlinePlayers.contains(player)) {
            outlinePlayers.remove(player);
            outlineCorner1.remove(player);
            outlineCorner2.remove(player);
            return false;
        }
        autoClaimPlayers.remove(player);
        autoUnclaimPlayers.remove(player);
        outlineCorner1.remove(player);
        outlineCorner2.remove(player);
        outlinePlayers.add(player);
        return true;
    }

    public boolean isOutlining(UUID player) { return outlinePlayers.contains(player); }

    public void updateOutline(UUID player, int chunkX, int chunkZ) {
        if (!outlineCorner1.containsKey(player)) {
            outlineCorner1.put(player, new int[]{chunkX, chunkZ});
            outlineCorner2.put(player, new int[]{chunkX, chunkZ});
        } else {
            int[] c1 = outlineCorner1.get(player);
            int[] c2 = outlineCorner2.get(player);
            c1[0] = Math.min(c1[0], chunkX);
            c1[1] = Math.min(c1[1], chunkZ);
            c2[0] = Math.max(c2[0], chunkX);
            c2[1] = Math.max(c2[1], chunkZ);
        }
    }

    public int[] getOutlineCorner1(UUID player) { return outlineCorner1.get(player); }
    public int[] getOutlineCorner2(UUID player) { return outlineCorner2.get(player); }

    public int getOutlineChunkCount(UUID player) {
        int[] c1 = outlineCorner1.get(player);
        int[] c2 = outlineCorner2.get(player);
        if (c1 == null || c2 == null) return 0;
        return (c2[0] - c1[0] + 1) * (c2[1] - c1[1] + 1);
    }

    public void clearOutline(UUID player) {
        outlinePlayers.remove(player);
        outlineCorner1.remove(player);
        outlineCorner2.remove(player);
    }

    public Optional<Land> getLandAt(String world, int chunkX, int chunkZ) {
        return Optional.ofNullable(claimedLands.get(generateKey(world, chunkX, chunkZ)));
    }

    private String generateKey(String world, int x, int z) {
        return world + ";" + x + ";" + z;
    }

    public void saveAll() {
        if (config == null) return;
        config.set("claims", null);
        for (Map.Entry<String, Land> entry : claimedLands.entrySet()) {
            String key = entry.getKey();
            Land land = entry.getValue();
            String path = "claims." + key;
            config.set(path + ".owner", land.getOwner().toString());
            config.set(path + ".world", land.getWorldName());
            config.set(path + ".x", land.getChunkX());
            config.set(path + ".z", land.getChunkZ());

            for (Map.Entry<String, Boolean> flagEntry : land.getFlags().entrySet()) {
                config.set(path + ".flags." + flagEntry.getKey(), flagEntry.getValue());
            }

            List<String> trustedList = new ArrayList<>();
            for (UUID uuid : land.getTrusted()) {
                trustedList.add(uuid.toString());
            }
            config.set(path + ".trusted", trustedList);
        }
        saveFile();
    }

    private void saveSingleLand(String key, Land land) {
        String path = "claims." + key;
        config.set(path + ".owner", land.getOwner().toString());
        config.set(path + ".world", land.getWorldName());
        config.set(path + ".x", land.getChunkX());
        config.set(path + ".z", land.getChunkZ());

        for (Map.Entry<String, Boolean> flagEntry : land.getFlags().entrySet()) {
            config.set(path + ".flags." + flagEntry.getKey(), flagEntry.getValue());
        }

        List<String> trustedList = new ArrayList<>();
        for (UUID uuid : land.getTrusted()) {
            trustedList.add(uuid.toString());
        }
        config.set(path + ".trusted", trustedList);
        saveFile();
    }

    private void loadAll() {
        if (!file.exists()) return;

        claimedLands.clear();
        var section = config.getConfigurationSection("claims");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "claims." + key;
            String ownerStr = config.getString(path + ".owner");
            String world = config.getString(path + ".world");
            int x = config.getInt(path + ".x");
            int z = config.getInt(path + ".z");

            if (ownerStr == null || world == null) continue;

            UUID owner = UUID.fromString(ownerStr);
            Land land = new Land(owner, world, x, z);

            var flagsSection = config.getConfigurationSection(path + ".flags");
            if (flagsSection != null) {
                for (String flagName : flagsSection.getKeys(false)) {
                    land.setFlag(flagName, config.getBoolean(path + ".flags." + flagName));
                }
            }

            List<String> trustedList = config.getStringList(path + ".trusted");
            for (String uuidStr : trustedList) {
                try {
                    land.addTrusted(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }

            claimedLands.put(key, land);
        }
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            BellLands.getInstance().getLogger().severe("Nie udalo sie zapisac pliku lands.yml: " + e.getMessage());
        }
    }
}
