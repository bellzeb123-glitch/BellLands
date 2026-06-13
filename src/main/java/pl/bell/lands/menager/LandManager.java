package pl.bell.lands.manager;

import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandManager {

    // Klucz: "worldName;chunkX;chunkZ"
    private final Map<String, Land> claimedLands = new HashMap<>();
    private File file;
    private FileConfiguration config;

    public void init() {
        File folder = BellLands.getInstance().getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.file = new File(folder, "lands.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        loadAll();
    }

    public void claimLand(Land land) {
        String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        claimedLands.put(key, land);
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

    private String generateKey(String world, int x, int z) {
        return world + ";" + x + ";" + z;
    }

    public void saveAll() {
        if (config == null) return;
        config.set("claims", null); // Wyczysc stare wpisy
        for (Map.Entry<String, Land> entry : claimedLands.entrySet()) {
            String key = entry.getKey();
            Land land = entry.getValue();
            String path = "claims." + key;
            config.set(path + ".owner", land.getOwner().toString());
            config.set(path + ".world", land.getWorldName());
            config.set(path + ".x", land.getChunkX());
            config.set(path + ".z", land.getChunkZ());
            
            // Zapisz flagi
            for (Map.Entry<String, Boolean> flagEntry : land.getFlags().entrySet()) {
                config.set(path + ".flags." + flagEntry.getKey(), flagEntry.getValue());
            }

            // Zapisz zaufanych
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

            // Załaduj flagi
            var flagsSection = config.getConfigurationSection(path + ".flags");
            if (flagsSection != null) {
                for (String flagName : flagsSection.getKeys(false)) {
                    land.setFlag(flagName, config.getBoolean(path + ".flags." + flagName));
                }
            }

            // Załaduj zaufanych
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