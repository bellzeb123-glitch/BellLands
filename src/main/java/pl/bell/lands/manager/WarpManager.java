package pl.bell.lands.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import pl.bell.lands.BellLands;
import pl.bell.lands.storage.Database;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Level;

public class WarpManager {

    private final Map<UUID, Map<String, Location>> warps = new HashMap<>();
    private Database db;

    public void init() {
        this.db = BellLands.getInstance().getLandManager().getDatabase();
        loadAll();
    }

    public boolean setWarp(UUID owner, String name, Location location) {
        warps.computeIfAbsent(owner, k -> new LinkedHashMap<>()).put(name.toLowerCase(), location);
        saveWarp(owner, name.toLowerCase(), location);
        return true;
    }

    public boolean deleteWarp(UUID owner, String name) {
        Map<String, Location> playerWarps = warps.get(owner);
        if (playerWarps == null) return false;
        boolean removed = playerWarps.remove(name.toLowerCase()) != null;
        if (removed) deleteWarpRow(owner, name.toLowerCase());
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

    private static final String UPSERT_SQL =
        "INSERT INTO warps(owner, name, world, x, y, z, yaw, pitch) VALUES(?,?,?,?,?,?,?,?) " +
        "ON CONFLICT(owner, name) DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y, " +
        "z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch";

    private void saveWarp(UUID owner, String name, Location loc) {
        String ownerStr = owner.toString();
        String world = loc.getWorld().getName();
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        float yaw = loc.getYaw(), pitch = loc.getPitch();
        db.async(() -> {
            try (PreparedStatement ps = db.conn().prepareStatement(UPSERT_SQL)) {
                ps.setString(1, ownerStr);
                ps.setString(2, name);
                ps.setString(3, world);
                ps.setDouble(4, x);
                ps.setDouble(5, y);
                ps.setDouble(6, z);
                ps.setDouble(7, yaw);
                ps.setDouble(8, pitch);
                ps.executeUpdate();
            } catch (SQLException e) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to save warp " + name, e);
            }
        });
    }

    private void deleteWarpRow(UUID owner, String name) {
        String ownerStr = owner.toString();
        db.async(() -> {
            try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM warps WHERE owner = ? AND name = ?")) {
                ps.setString(1, ownerStr);
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to delete warp " + name, e);
            }
        });
    }

    /** Persists all warps in one transaction (used on shutdown for safety). */
    public void flushAll() {
        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Location>> e : warps.entrySet()) {
            String ownerStr = e.getKey().toString();
            for (Map.Entry<String, Location> w : e.getValue().entrySet()) {
                Location loc = w.getValue();
                rows.add(new Object[]{ownerStr, w.getKey(), loc.getWorld().getName(),
                    loc.getX(), loc.getY(), loc.getZ(), (double) loc.getYaw(), (double) loc.getPitch()});
            }
        }
        db.async(() -> {
            try {
                db.conn().setAutoCommit(false);
                try (PreparedStatement ps = db.conn().prepareStatement(UPSERT_SQL)) {
                    for (Object[] r : rows) {
                        ps.setString(1, (String) r[0]);
                        ps.setString(2, (String) r[1]);
                        ps.setString(3, (String) r[2]);
                        ps.setDouble(4, (double) r[3]);
                        ps.setDouble(5, (double) r[4]);
                        ps.setDouble(6, (double) r[5]);
                        ps.setDouble(7, (double) r[6]);
                        ps.setDouble(8, (double) r[7]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                db.conn().commit();
            } catch (SQLException ex) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to flush warps", ex);
            } finally {
                try { db.conn().setAutoCommit(true); } catch (SQLException ignored) {}
            }
        });
    }

    private void loadAll() {
        warps.clear();
        try (Statement st = db.conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT owner, name, world, x, y, z, yaw, pitch FROM warps")) {
            while (rs.next()) {
                UUID owner;
                try {
                    owner = UUID.fromString(rs.getString("owner"));
                } catch (IllegalArgumentException e) {
                    continue;
                }
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) continue;
                Location loc = new Location(world, rs.getDouble("x"), rs.getDouble("y"), rs.getDouble("z"),
                    (float) rs.getDouble("yaw"), (float) rs.getDouble("pitch"));
                warps.computeIfAbsent(owner, k -> new LinkedHashMap<>()).put(rs.getString("name"), loc);
            }
        } catch (SQLException e) {
            BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to load warps", e);
        }

        if (warps.isEmpty()) {
            importLegacyYaml();
        }
    }

    /** One-time import of an existing warps.yml into the DB. */
    private void importLegacyYaml() {
        File legacy = new File(BellLands.getInstance().getDataFolder(), "warps.yml");
        if (!legacy.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(legacy);
        var section = config.getConfigurationSection("warps");
        if (section == null) return;

        int imported = 0;
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

                Location loc = new Location(world,
                    section.getDouble(path + ".x"), section.getDouble(path + ".y"), section.getDouble(path + ".z"),
                    (float) section.getDouble(path + ".yaw"), (float) section.getDouble(path + ".pitch"));
                playerWarps.put(warpName, loc);
                imported++;
            }
            if (!playerWarps.isEmpty()) {
                warps.put(owner, playerWarps);
            }
        }

        if (imported > 0) {
            flushAll();
            File backup = new File(BellLands.getInstance().getDataFolder(), "warps.yml.imported");
            legacy.renameTo(backup);
            BellLands.getInstance().getLogger().info("Imported " + imported + " warps from warps.yml into the database");
        }
    }
}
