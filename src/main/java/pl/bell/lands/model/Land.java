package pl.bell.lands.model;

import pl.bell.lands.BellLands;

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

    public static final String[] ALL_FLAGS = {
        "pvp", "explosions", "explosion-damage", "fire-spread", "mob-spawning", "mob-damage",
        "lava-flow", "lava-damage", "water-flow", "piston", "leaf-decay"
    };

    // Guest permissions: what strangers (non-owner, non-trusted) may do on a claim.
    // All default to false (blocked) — the owner opens up access deliberately.
    public static final String[] GUEST_FLAGS = {
        "guest-doors", "guest-use", "guest-chest", "guest-frames", "guest-animals"
    };

    public Land(UUID owner, String worldName, int chunkX, int chunkZ) {
        this.owner = owner;
        this.worldName = worldName;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.flags = new HashMap<>();
        this.trusted = new HashSet<>();

        var config = BellLands.getInstance().getConfig();
        for (String flag : ALL_FLAGS) {
            boolean def = config.getBoolean("claims.default-flags." + flag, getHardcodedDefault(flag));
            flags.put(flag, def);
        }
        for (String gf : GUEST_FLAGS) {
            boolean def = config.getBoolean("claims.default-flags." + gf, false);
            flags.put(gf, def);
        }
    }

    private static boolean getHardcodedDefault(String flag) {
        return switch (flag) {
            case "mob-spawning", "mob-damage", "leaf-decay" -> true;
            default -> false;
        };
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

    public static boolean isValidFlag(String flag) {
        for (String f : ALL_FLAGS) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        for (String f : GUEST_FLAGS) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }

    public static boolean isGuestFlag(String flag) {
        for (String f : GUEST_FLAGS) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }
}
