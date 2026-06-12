package pl.bell.lands.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Land {

    private final UUID owner;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final Map<String, Boolean> flags;

    public Land(UUID owner, String worldName, int chunkX, int chunkZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.flags = new HashMap<>();
        // Domyślne flagi (można rozbudować)
        flags.put("pvp", false);
        flags.put("explosions", false);
    }

    public UUID getOwner() { return owner; }
    public String getWorldName() { return worldName; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean getFlag(String flag) { return flags.getOrDefault(flag, false); }
    public void setFlag(String flag, boolean value) { flags.put(flag, value); }
}