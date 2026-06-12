package pl.bell.lands.manager;

import org.bukkit.Chunk;
import pl.bell.lands.model.Land;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LandManager {

    // Klucz: "worldName;chunkX;chunkZ"
    private final Map<String, Land> claimedLands = new HashMap<>();

    public void claimLand(Land land) {
        String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        claimedLands.put(key, land);
    }

    public void unclaimLand(String world, int x, int z) {
        claimedLands.remove(generateKey(world, x, z));
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
}