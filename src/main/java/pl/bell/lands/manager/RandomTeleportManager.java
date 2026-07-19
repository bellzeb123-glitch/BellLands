package pl.bell.lands.manager;

import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Random teleport (/tpr) — finds a safe spot within configured radius.
 */
public class RandomTeleportManager {

    private final BellLands plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private boolean enabled;
    private long cooldownMs;
    private int maxAttempts;
    private int minRadius;
    private int maxRadius;
    private boolean useWorldSpawnAsCenter;
    private double centerX;
    private double centerZ;
    private boolean avoidClaims;
    private int minY;
    private int maxY;
    private List<String> allowedWorlds;

    public RandomTeleportManager(BellLands plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        this.enabled = plugin.getConfig().getBoolean("tpr.enabled", true);
        this.cooldownMs = plugin.getConfig().getLong("tpr.cooldown-seconds", 60) * 1000L;
        this.maxAttempts = Math.max(1, plugin.getConfig().getInt("tpr.max-attempts", 16));
        this.minRadius = Math.max(0, plugin.getConfig().getInt("tpr.min-radius", 100));
        this.maxRadius = Math.max(minRadius, plugin.getConfig().getInt("tpr.max-radius", 5000));
        this.useWorldSpawnAsCenter = plugin.getConfig().getBoolean("tpr.use-world-spawn-as-center", true);
        this.centerX = plugin.getConfig().getDouble("tpr.center-x", 0);
        this.centerZ = plugin.getConfig().getDouble("tpr.center-z", 0);
        this.avoidClaims = plugin.getConfig().getBoolean("tpr.avoid-claims", true);
        this.minY = plugin.getConfig().getInt("tpr.min-y", 40);
        this.maxY = plugin.getConfig().getInt("tpr.max-y", 320);
        this.allowedWorlds = plugin.getConfig().getStringList("tpr.worlds");
    }

    public void teleport(Player player) {
        LangManager lang = plugin.getLangManager();

        if (!enabled) {
            player.sendMessage(lang.component("tpr-disabled"));
            return;
        }

        World world = player.getWorld();
        if (!isWorldAllowed(world)) {
            player.sendMessage(lang.component("tpr-world-denied"));
            return;
        }

        if (!player.hasPermission("belllands.tpr.bypass")) {
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(player.getUniqueId());
            if (last != null && now - last < cooldownMs) {
                long remaining = (cooldownMs - (now - last) + 999) / 1000;
                player.sendMessage(lang.component("tpr-cooldown", "seconds", String.valueOf(remaining)));
                return;
            }
        }

        player.sendMessage(lang.component("tpr-searching"));

        double cx = useWorldSpawnAsCenter ? world.getSpawnLocation().getX() : centerX;
        double cz = useWorldSpawnAsCenter ? world.getSpawnLocation().getZ() : centerZ;

        Location found = findSafeLocation(world, cx, cz);
        if (found == null) {
            player.sendMessage(lang.component("tpr-failed"));
            return;
        }

        if (!player.hasPermission("belllands.tpr.bypass")) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        found.setYaw(player.getLocation().getYaw());
        found.setPitch(player.getLocation().getPitch());
        player.teleport(found);
        player.sendMessage(lang.component("tpr-success",
                "x", String.valueOf(found.getBlockX()),
                "y", String.valueOf(found.getBlockY()),
                "z", String.valueOf(found.getBlockZ())));
    }

    private boolean isWorldAllowed(World world) {
        if (allowedWorlds == null || allowedWorlds.isEmpty()) return true;
        for (String name : allowedWorlds) {
            if (world.getName().equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private Location findSafeLocation(World world, double centerX, double centerZ) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            int radius = minRadius + (maxRadius > minRadius
                    ? rng.nextInt(maxRadius - minRadius + 1)
                    : 0);
            int x = (int) Math.round(centerX + Math.cos(angle) * radius);
            int z = (int) Math.round(centerZ + Math.sin(angle) * radius);

            if (avoidClaims) {
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                if (plugin.getLandManager().isClaimed(world.getChunkAt(chunkX, chunkZ))) {
                    continue;
                }
            }

            int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (y < minY || y > maxY) continue;

            Block ground = world.getBlockAt(x, y, z);
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);

            if (!isSafeGround(ground.getType())) continue;
            if (!feet.getType().isAir() || !head.getType().isAir()) continue;

            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }
        return null;
    }

    private boolean isSafeGround(Material type) {
        if (!type.isSolid()) return false;
        return switch (type) {
            case LAVA, MAGMA_BLOCK, CACTUS, FIRE, SOUL_FIRE, CAMPFIRE, SOUL_CAMPFIRE,
                 SWEET_BERRY_BUSH, POWDER_SNOW, WITHER_ROSE -> false;
            default -> true;
        };
    }
}
