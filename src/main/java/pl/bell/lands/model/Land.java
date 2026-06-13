package pl.bell.lands.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Land {

    private final UUID owner;
    private final String worldName;
    private final int chunkX;
    private final int chunkZ;
    private final Map<String, Boolean> flags;
    private final Set<UUID> trusted;

    public Land(UUID owner, String worldName, int chunkX, int chunkZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.flags = new HashMap<>();
        this.trusted = new HashSet<>();
        // Domyslne flagi
        flags.put("pvp", false);
        flags.put("explosions", false);
    }

    public UUID getOwner() { return owner; }
    public String getWorldName() { return worldName; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean getFlag(String flag) { return flags.getOrDefault(flag, false); }
    public void setFlag(String flag, boolean value) { flags.put(flag, value); }
    public Map<String, Boolean> getFlags() { return flags; }

    public Set<UUID> getTrusted() { return trusted; }
    public boolean isTrusted(UUID uuid) { return trusted.contains(uuid); }
    public void addTrusted(UUID uuid) { trusted.add(uuid); }
    public void removeTrusted(UUID uuid) { trusted.remove(uuid); }
}