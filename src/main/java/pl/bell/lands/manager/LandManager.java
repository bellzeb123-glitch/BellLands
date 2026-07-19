package pl.bell.lands.manager;

import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.storage.Database;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class LandManager {

    private final Map<String, Land> claimedLands = new HashMap<>();
    private final Map<UUID, Integer> claimCounts = new HashMap<>();
    private final Set<UUID> autoClaimPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> autoUnclaimPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, int[]> outlineCorner1 = new HashMap<>();
    private final Map<UUID, int[]> outlineCorner2 = new HashMap<>();
    /** Świat, w którym rozpoczęto outline — fill/cząsteczki tylko tu. */
    private final Map<UUID, String> outlineWorld = new HashMap<>();
    private final Set<UUID> outlinePlayers = ConcurrentHashMap.newKeySet();
    private Database db;
    private File limitsFile;
    private FileConfiguration limitsConfig;

    public void init() {
        File folder = BellLands.getInstance().getDataFolder();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.db = new Database();
        try {
            db.init(new File(folder, "data.db"));
        } catch (Exception e) {
            BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to open database", e);
            throw new IllegalStateException("BellLands database init failed", e);
        }
        this.limitsFile = new File(folder, "claim-limits.yml");
        this.limitsConfig = YamlConfiguration.loadConfiguration(limitsFile);
        loadAll();
    }

    // Admins with claim/region bypass turned OFF (default: bypass ON for those with permission).
    private final Set<UUID> bypassDisabled = ConcurrentHashMap.newKeySet();

    /** Whether the player currently ignores claim/region protection (admin bypass). */
    public boolean hasBypass(org.bukkit.entity.Player player) {
        if (!player.hasPermission("belllands.admin") && !player.isOp()) return false;
        return !bypassDisabled.contains(player.getUniqueId());
    }

    /** Toggles admin bypass for a player. Returns the new state (true = bypass now ON). */
    public boolean toggleBypass(UUID player) {
        if (bypassDisabled.remove(player)) return true; // re-enabled
        bypassDisabled.add(player);
        return false; // now disabled
    }

    public Database getDatabase() {
        return db;
    }

    public void shutdown() {
        if (db != null) db.shutdown();
    }

    public Collection<Land> getAllLands() {
        return claimedLands.values();
    }

    public void claimLand(Land land) {
        String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        Land previous = claimedLands.put(key, land);
        if (previous != null) claimCounts.merge(previous.getOwner(), -1, Integer::sum);
        claimCounts.merge(land.getOwner(), 1, Integer::sum);
        saveSingleLand(key, land);
    }

    public void saveLand(Land land) {
        String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        saveSingleLand(key, land);
    }

    /**
     * Applies a flag value to every claim (used when admin default flags change).
     * Addons may exclude chunks (e.g. named-claim chunks) via {@link #applyFlagExcludingChunks}.
     */
    public void applyFlagToAllClaims(String flag, boolean value) {
        applyFlagExcludingChunks(flag, value, java.util.Collections.emptySet());
    }

    /**
     * Applies a flag to all claims except those whose chunk key is in {@code excludeChunkKeys}.
     * Chunk key format: {@code world;x;z}.
     */
    public void applyFlagExcludingChunks(String flag, boolean value, java.util.Set<String> excludeChunkKeys) {
        for (Land land : claimedLands.values()) {
            String key = generateKey(land.getWorldName(), land.getChunkX(), land.getChunkZ());
            if (excludeChunkKeys.contains(key)) continue;
            land.setFlag(flag, value);
        }
        saveAll();
        pl.bell.lands.integration.Pl3xMapHook.drawAll();
    }

    /**
     * Spreads a guest flag across the owner's connected (4-neighbour) claim group,
     * starting from the given chunk. Standalone fallback when no addon propagator is set.
     */
    public void applyGuestFlagToGroup(UUID owner, String world, int startX, int startZ,
                                      String flag, boolean value) {
        Set<Long> visited = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startZ});
        visited.add(((long) startX << 32) | (startZ & 0xFFFFFFFFL));

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            Optional<Land> opt = getLandAt(world, pos[0], pos[1]);
            if (opt.isEmpty() || !opt.get().getOwner().equals(owner)) continue;

            opt.get().setFlag(flag, value);
            saveLand(opt.get());

            int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] d : dirs) {
                int nx = pos[0] + d[0], nz = pos[1] + d[1];
                long key = ((long) nx << 32) | (nz & 0xFFFFFFFFL);
                if (visited.add(key)) queue.add(new int[]{nx, nz});
            }
        }
    }

    public void unclaimLand(String world, int x, int z) {
        String key = generateKey(world, x, z);
        Land removed = claimedLands.remove(key);
        if (removed != null) claimCounts.merge(removed.getOwner(), -1, Integer::sum);
        db.async(() -> {
            try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM claims WHERE chunk_key = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            } catch (SQLException e) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to delete claim " + key, e);
            }
        });
    }

    public Optional<Land> getLandAt(Chunk chunk) {
        String key = generateKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        return Optional.ofNullable(claimedLands.get(key));
    }

    public boolean isClaimed(Chunk chunk) {
        return getLandAt(chunk).isPresent();
    }

    public int getClaimCount(UUID owner) {
        return claimCounts.getOrDefault(owner, 0);
    }

    public int getMaxClaims(org.bukkit.entity.Player player) {
        Integer resolved = BellLands.resolveMaxClaims(player);
        if (resolved != null) return resolved;

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
        clearOutline(player);
    }

    // ── Outline mode ────────────────────────────────────────
    public boolean toggleOutline(UUID player) {
        if (outlinePlayers.contains(player)) {
            clearOutline(player);
            return false;
        }
        autoClaimPlayers.remove(player);
        autoUnclaimPlayers.remove(player);
        clearOutline(player);
        outlinePlayers.add(player);
        return true;
    }

    public boolean isOutlining(UUID player) { return outlinePlayers.contains(player); }

    /**
     * Rozszerza zaznaczenie outline. {@code world} musi być stały — zmiana świata czyści outline.
     */
    public void updateOutline(UUID player, String world, int chunkX, int chunkZ) {
        if (world == null || world.isBlank()) return;
        String existing = outlineWorld.get(player);
        if (existing != null && !existing.equals(world)) {
            // Gracz zmienił wymiar — nie mieszaj koordów między światami
            outlineCorner1.remove(player);
            outlineCorner2.remove(player);
            outlineWorld.put(player, world);
        } else if (existing == null) {
            outlineWorld.put(player, world);
        }
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

    /** @deprecated use {@link #updateOutline(UUID, String, int, int)} */
    @Deprecated
    public void updateOutline(UUID player, int chunkX, int chunkZ) {
        updateOutline(player, outlineWorld.getOrDefault(player, ""), chunkX, chunkZ);
    }

    public int[] getOutlineCorner1(UUID player) { return outlineCorner1.get(player); }
    public int[] getOutlineCorner2(UUID player) { return outlineCorner2.get(player); }
    public String getOutlineWorld(UUID player) { return outlineWorld.get(player); }

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
        outlineWorld.remove(player);
    }

    public Optional<Land> getLandAt(String world, int chunkX, int chunkZ) {
        if (world == null) return Optional.empty();
        Land land = claimedLands.get(generateKey(world, chunkX, chunkZ));
        if (land == null) return Optional.empty();
        // Ochrona przed uszkodzonymi kluczami DB (chunk_key ≠ world+x+z)
        if (!world.equals(land.getWorldName())) {
            BellLands.getInstance().getLogger().warning(
                "[Claims] Klucz/świat niespójny: lookup=" + world
                    + " land.world=" + land.getWorldName()
                    + " @" + chunkX + "," + chunkZ + " — ignoruję.");
            return Optional.empty();
        }
        return Optional.of(land);
    }

    /**
     * Czy w tym świecie wolno claimować.
     * {@code claims.disabled-worlds} ma pierwszeństwo; {@code claims.worlds} = whitelist (pusta = wszystkie).
     * End i Nether są dozwolone domyślnie.
     */
    public boolean isClaimWorldAllowed(String worldName) {
        if (worldName == null || worldName.isBlank()) return false;
        var cfg = BellLands.getInstance().getConfig();
        java.util.List<String> disabled = cfg.getStringList("claims.disabled-worlds");
        for (String d : disabled) {
            if (worldName.equalsIgnoreCase(d)) return false;
        }
        java.util.List<String> allowed = cfg.getStringList("claims.worlds");
        if (allowed == null || allowed.isEmpty()) return true;
        for (String a : allowed) {
            if (worldName.equalsIgnoreCase(a)) return true;
        }
        return false;
    }

    private String generateKey(String world, int x, int z) {
        return world + ";" + x + ";" + z;
    }

    /** Persists every claim in one async transaction. Used for global operations (rare). */
    public void saveAll() {
        // Snapshot on the calling thread so the writer never touches mutable Land objects.
        List<Object[]> rows = new ArrayList<>(claimedLands.size());
        for (Map.Entry<String, Land> entry : claimedLands.entrySet()) {
            Land land = entry.getValue();
            rows.add(new Object[]{
                entry.getKey(), land.getOwner().toString(), land.getWorldName(),
                land.getChunkX(), land.getChunkZ(), serializeFlags(land), serializeTrusted(land)
            });
        }
        db.async(() -> {
            try {
                db.conn().setAutoCommit(false);
                try (PreparedStatement ps = db.conn().prepareStatement(UPSERT_SQL)) {
                    for (Object[] r : rows) {
                        ps.setString(1, (String) r[0]);
                        ps.setString(2, (String) r[1]);
                        ps.setString(3, (String) r[2]);
                        ps.setInt(4, (int) r[3]);
                        ps.setInt(5, (int) r[4]);
                        ps.setString(6, (String) r[5]);
                        ps.setString(7, (String) r[6]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                db.conn().commit();
            } catch (SQLException e) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to save all claims", e);
            } finally {
                try { db.conn().setAutoCommit(true); } catch (SQLException ignored) {}
            }
        });
    }

    private static final String UPSERT_SQL =
        "INSERT INTO claims(chunk_key, owner, world, x, z, flags, trusted) VALUES(?,?,?,?,?,?,?) " +
        "ON CONFLICT(chunk_key) DO UPDATE SET owner=excluded.owner, world=excluded.world, " +
        "x=excluded.x, z=excluded.z, flags=excluded.flags, trusted=excluded.trusted";

    private void saveSingleLand(String key, Land land) {
        // Serialize on the calling thread; the captured strings are immutable.
        String owner = land.getOwner().toString();
        String world = land.getWorldName();
        int x = land.getChunkX();
        int z = land.getChunkZ();
        String flags = serializeFlags(land);
        String trusted = serializeTrusted(land);
        db.async(() -> {
            try (PreparedStatement ps = db.conn().prepareStatement(UPSERT_SQL)) {
                ps.setString(1, key);
                ps.setString(2, owner);
                ps.setString(3, world);
                ps.setInt(4, x);
                ps.setInt(5, z);
                ps.setString(6, flags);
                ps.setString(7, trusted);
                ps.executeUpdate();
            } catch (SQLException e) {
                BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to save claim " + key, e);
            }
        });
    }

    private void loadAll() {
        claimedLands.clear();
        claimCounts.clear();
        int repaired = 0;

        try (Statement st = db.conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT chunk_key, owner, world, x, z, flags, trusted FROM claims")) {
            while (rs.next()) {
                String ownerStr = rs.getString("owner");
                String world = rs.getString("world");
                if (ownerStr == null || world == null) continue;

                UUID owner;
                try {
                    owner = UUID.fromString(ownerStr);
                } catch (IllegalArgumentException e) {
                    continue;
                }

                int x = rs.getInt("x");
                int z = rs.getInt("z");
                String expectedKey = generateKey(world, x, z);
                String storedKey = rs.getString("chunk_key");

                Land land = new Land(owner, world, x, z);
                applyFlags(land, rs.getString("flags"));
                applyTrusted(land, rs.getString("trusted"));

                // Zawsze klucz z world+x+z — nie ufaj raw chunk_key (mogło być bez świata)
                claimedLands.put(expectedKey, land);
                claimCounts.merge(owner, 1, Integer::sum);

                if (storedKey == null || !storedKey.equals(expectedKey)) {
                    repaired++;
                    final String bad = storedKey;
                    final String good = expectedKey;
                    final String o = ownerStr;
                    final String w = world;
                    final String flags = rs.getString("flags");
                    final String trusted = rs.getString("trusted");
                    db.async(() -> {
                        try {
                            if (bad != null && !bad.equals(good)) {
                                try (PreparedStatement del = db.conn().prepareStatement(
                                        "DELETE FROM claims WHERE chunk_key = ?")) {
                                    del.setString(1, bad);
                                    del.executeUpdate();
                                }
                            }
                            try (PreparedStatement ps = db.conn().prepareStatement(UPSERT_SQL)) {
                                ps.setString(1, good);
                                ps.setString(2, o);
                                ps.setString(3, w);
                                ps.setInt(4, x);
                                ps.setInt(5, z);
                                ps.setString(6, flags != null ? flags : "");
                                ps.setString(7, trusted != null ? trusted : "");
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            BellLands.getInstance().getLogger().log(Level.SEVERE,
                                "Failed to repair claim key " + bad + " -> " + good, e);
                        }
                    });
                }
            }
        } catch (SQLException e) {
            BellLands.getInstance().getLogger().log(Level.SEVERE, "Failed to load claims", e);
        }

        if (repaired > 0) {
            BellLands.getInstance().getLogger().warning(
                "[Claims] Naprawiono " + repaired + " niespójnych kluczy chunk_key (świat+koordy).");
        }

        if (claimedLands.isEmpty()) {
            importLegacyYaml();
        }

        if (BellLands.getInstance().getConfig().getBoolean("claims.cleanup-crossworld-duplicates", false)) {
            int removed = cleanupCrossWorldDuplicates(false);
            if (removed > 0) {
                BellLands.getInstance().getLogger().warning(
                    "[Claims] Usunięto " + removed
                        + " starych duplikatów (Nether/End = kopia overworld). "
                        + "Nowe claimy w End/Nether na tych samych X/Z są dozwolone.");
                pl.bell.lands.integration.Pl3xMapHook.drawAll();
            }
        }
    }

    /**
     * Usuwa claimy w {@code *_nether} / {@code *_the_end}, gdy ten sam właściciel
     * ma już claim na tych samych chunk X/Z w overworldzie (sibling bez sufiksu).
     * Typowy ślad buga outline bez świata.
     * <p>
     * Nie blokuje samodzielnych claimów w End/Nether — tylko kasuje duplikaty względem overworld.
     *
     * @param dryRun gdy true - tylko zlicza, nic nie kasuje
     * @return liczba usunietych (lub wykrytych przy dryRun)
     */
    public int cleanupCrossWorldDuplicates(boolean dryRun) {
        List<Land> toRemove = new ArrayList<>();
        for (Land land : claimedLands.values()) {
            String sibling = overworldSiblingOf(land.getWorldName());
            if (sibling == null) continue;
            Optional<Land> over = getLandAt(sibling, land.getChunkX(), land.getChunkZ());
            if (over.isEmpty()) continue;
            if (!over.get().getOwner().equals(land.getOwner())) continue;
            toRemove.add(land);
        }
        if (dryRun) return toRemove.size();
        for (Land land : toRemove) {
            unclaimLand(land.getWorldName(), land.getChunkX(), land.getChunkZ());
        }
        return toRemove.size();
    }

    /** {@code world_the_end} → {@code world}; {@code world_nether} → {@code world}; inaczej null. */
    private static String overworldSiblingOf(String world) {
        if (world == null) return null;
        if (world.endsWith("_the_end")) {
            return world.substring(0, world.length() - "_the_end".length());
        }
        if (world.endsWith("_nether")) {
            return world.substring(0, world.length() - "_nether".length());
        }
        return null;
    }

    /** One-time import of an existing lands.yml (e.g. after upgrade or LCP migration) into the DB. */
    private void importLegacyYaml() {
        File folder = BellLands.getInstance().getDataFolder();
        File legacy = new File(folder, "lands.yml");
        if (!legacy.exists()) return;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(legacy);
        var section = yaml.getConfigurationSection("claims");
        if (section == null) return;

        int imported = 0;
        for (String key : section.getKeys(false)) {
            String path = "claims." + key;
            String ownerStr = yaml.getString(path + ".owner");
            String world = yaml.getString(path + ".world");
            if (ownerStr == null || world == null) continue;

            UUID owner;
            try {
                owner = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException e) {
                continue;
            }

            Land land = new Land(owner, world, yaml.getInt(path + ".x"), yaml.getInt(path + ".z"));
            var flagsSection = yaml.getConfigurationSection(path + ".flags");
            if (flagsSection != null) {
                for (String flagName : flagsSection.getKeys(false)) {
                    land.setFlag(flagName, yaml.getBoolean(path + ".flags." + flagName));
                }
            }
            for (String uuidStr : yaml.getStringList(path + ".trusted")) {
                try {
                    land.addTrusted(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }

            claimedLands.put(key, land);
            claimCounts.merge(owner, 1, Integer::sum);
            imported++;
        }

        if (imported > 0) {
            saveAll();
            File backup = new File(folder, "lands.yml.imported");
            if (legacy.renameTo(backup)) {
                BellLands.getInstance().getLogger().info("Imported " + imported + " claims from lands.yml into the database (backup: lands.yml.imported)");
            } else {
                BellLands.getInstance().getLogger().info("Imported " + imported + " claims from lands.yml into the database (could not rename original)");
            }
        }
    }

    // ── Serialization helpers ────────────────────────────────
    private static String serializeFlags(Land land) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : land.getFlags().entrySet()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(e.getKey()).append('=').append(e.getValue() ? '1' : '0');
        }
        return sb.toString();
    }

    private static String serializeTrusted(Land land) {
        StringBuilder sb = new StringBuilder();
        for (UUID uuid : land.getTrusted()) {
            if (sb.length() > 0) sb.append(',');
            sb.append(uuid);
        }
        return sb.toString();
    }

    private static void applyFlags(Land land, String flags) {
        if (flags == null || flags.isEmpty()) return;
        for (String pair : flags.split(",")) {
            int eq = pair.lastIndexOf('=');
            if (eq <= 0) continue;
            land.setFlag(pair.substring(0, eq), pair.charAt(eq + 1) == '1');
        }
    }

    private static void applyTrusted(Land land, String trusted) {
        if (trusted == null || trusted.isEmpty()) return;
        for (String uuidStr : trusted.split(",")) {
            try {
                land.addTrusted(UUID.fromString(uuidStr.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
    }
}
