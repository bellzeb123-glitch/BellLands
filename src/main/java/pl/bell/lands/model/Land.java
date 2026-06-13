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

    // Lista wszystkich obslugiwanych flag
    public static final String[] ALL_FLAGS = {
        "pvp", "explosions", "fire-spread", "mob-spawning", "mob-damage",
        "lava-flow", "water-flow", "piston", "leaf-decay", "use"
    };

    public Land(UUID owner, String worldName, int chunkX, int chunkZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.flags = new HashMap<>();
        this.trusted = new HashSet<>();

        // Domyslne flagi — bezpieczne ustawienia (wszystko zablokowane)
        flags.put("pvp", false);           // PVP wylaczone
        flags.put("explosions", false);    // Wybuchy zablokowane
        flags.put("fire-spread", false);   // Rozprzestrzenianie ognia zablokowane
        flags.put("mob-spawning", true);   // Spawnowanie mobow wlaczone
        flags.put("mob-damage", true);     // Obrazenia od mobow wlaczone
        flags.put("lava-flow", false);     // Rozlewanie lawy zablokowane
        flags.put("water-flow", false);    // Rozlewanie wody zablokowane
        flags.put("piston", false);        // Tloki z zewnatrz zablokowane
        flags.put("leaf-decay", true);     // Rozpad lisci wlaczony
        flags.put("use", false);           // Interakcja obcych z blokami zablokowana
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

    /**
     * Sprawdza czy podana nazwa flagi jest prawidlowa.
     */
    public static boolean isValidFlag(String flag) {
        for (String f : ALL_FLAGS) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }
}