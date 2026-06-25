package pl.bell.lands.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import pl.bell.lands.BellLands;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.WarpManager;
import pl.bell.lands.model.Land;
import pl.bell.suite.api.ActionResult;
import pl.bell.suite.api.Actor;
import pl.bell.suite.api.SuiteAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Logika admina BellLands dla panelu BellSuite: widoki (strefy/warpy gracza, ustawienia globalne)
 * + wykonanie akcji. Akcje wolaja DOKLADNIE te same managery co GUI w grze (unclaimLand, setFlag+
 * saveLand, addTrusted/removeTrusted) — zero duplikacji logiki.
 */
public final class LandsAdmin {

    private final LandManager lands;
    private final WarpManager warps;

    public LandsAdmin(LandManager lands, WarpManager warps) {
        this.lands = lands;
        this.warps = warps;
    }

    // ── WIDOKI ──────────────────────────────────────────────

    /** Strefy + warpy + limit gracza. params: player=nick. */
    public String viewPlayer(String name) {
        if (name == null || name.isBlank()) return "{\"found\":false}";
        OfflinePlayer op = Bukkit.getOfflinePlayer(name);
        UUID uuid = op.getUniqueId();

        List<String> zoneJson = new ArrayList<>();
        for (ClaimZoneBuilder.Zone z : ClaimZoneBuilder.compute(lands.getAllLands())) {
            if (!z.owner().equals(uuid)) continue;
            // flagi + trusted z reprezentatywnego chunka strefy
            Land first = lands.getLandAt(z.world(), z.chunks()[0][0], z.chunks()[0][1]).orElse(null);
            zoneJson.add("{"
                    + "\"world\":\"" + esc(z.world()) + "\","
                    + "\"chunkCount\":" + z.chunkCount() + ","
                    + "\"centerX\":" + (long) z.centerX() + ",\"centerZ\":" + (long) z.centerZ() + ","
                    + "\"chunks\":\"" + chunksCsv(z.chunks()) + "\","
                    + "\"flags\":" + flagsJson(first) + ","
                    + "\"trusted\":" + trustedJson(first)
                    + "}");
        }

        List<String> warpJson = new ArrayList<>();
        for (String w : warps.getWarpNames(uuid)) {
            Location loc = warps.getWarp(uuid, w);
            if (loc == null || loc.getWorld() == null) continue;
            warpJson.add("{\"name\":\"" + esc(w) + "\",\"world\":\"" + esc(loc.getWorld().getName())
                    + "\",\"x\":" + (long) loc.getX() + ",\"z\":" + (long) loc.getZ() + "}");
        }

        int override = lands.getClaimLimitOverride(uuid);
        int claimLimit = override >= 0 ? override
                : BellLands.getInstance().getConfig().getInt("claims.max-per-player", 5);
        return "{"
                + "\"found\":true,"
                + "\"player\":\"" + esc(op.getName() != null ? op.getName() : name) + "\","
                + "\"uuid\":\"" + uuid + "\","
                + "\"limitOverride\":" + override + ","
                + "\"claimLimit\":" + claimLimit + ","
                + "\"zones\":" + arr(zoneJson) + ","
                + "\"warps\":" + arr(warpJson)
                + "}";
    }

    /** Ustawienia globalne BellLands: flagi, domyślne flagi, zablokowane flagi, particle, język. */
    public String viewSettings() {
        var cfg = BellLands.getInstance().getConfig();
        boolean particles = cfg.getBoolean("claims.particle-borders", true);
        String lang = cfg.getString("language", "en");

        // wszystkie flagi (ochrony + gości + home)
        List<String> all = new ArrayList<>();
        List<String> defaults = new ArrayList<>();
        for (String f : Land.ALL_FLAGS) {
            all.add("\"" + esc(f) + "\"");
            defaults.add("\"" + esc(f) + "\":" + cfg.getBoolean("claims.default-flags." + f, false));
        }
        List<String> guest = new ArrayList<>();
        for (String f : Land.GUEST_FLAGS) {
            guest.add("\"" + esc(f) + "\"");
            defaults.add("\"" + esc(f) + "\":" + cfg.getBoolean("claims.default-flags." + f, false));
        }
        List<String> home = new ArrayList<>();
        for (String f : Land.HOME_FLAGS) {
            home.add("\"" + esc(f) + "\"");
            defaults.add("\"" + esc(f) + "\":" + cfg.getBoolean("claims.default-flags." + f, false));
        }
        List<String> locked = new ArrayList<>();
        for (String f : cfg.getStringList("claims.locked-flags")) locked.add("\"" + esc(f) + "\"");

        return "{\"particles\":" + particles + ",\"language\":\"" + esc(lang) + "\","
                + "\"flags\":" + arr(all) + ",\"guestFlags\":" + arr(guest)
                + ",\"homeFlags\":" + arr(home) + ","
                + "\"defaults\":{" + String.join(",", defaults) + "},"
                + "\"locked\":" + arr(locked) + "}";
    }

    // ── AKCJE ───────────────────────────────────────────────

    public ActionResult invoke(SuiteAction a, Actor actor) {
        if (!actor.admin() && !actor.has("bellsuite.module.belllands")) {
            return ActionResult.error("Brak uprawnien.");
        }
        try {
            return switch (a.name()) {
                case "claim.delete" -> deleteChunks(a.param("world"), a.param("chunks"));
                case "claim.deleteAll" -> deleteAll(a.param("owner"));
                case "flag.set" -> setFlag(a.param("world"), a.param("chunks"),
                        a.param("flag"), "true".equalsIgnoreCase(a.param("value")));
                case "trust.add" -> setTrust(a.param("world"), a.param("chunks"), a.param("target"), true);
                case "trust.remove" -> setTrust(a.param("world"), a.param("chunks"), a.param("target"), false);
                case "warp.delete" -> deleteWarp(a.param("owner"), a.param("name"));
                case "limit.set" -> setLimit(a.param("owner"), a.param("value"));
                case "settings.particles" -> setParticles("true".equalsIgnoreCase(a.param("value")));
                case "settings.language" -> setLanguage(a.param("value"));
                case "settings.defaultFlag" -> setDefaultFlag(a.param("flag"), "true".equalsIgnoreCase(a.param("value")));
                case "settings.lockedFlag" -> setLockedFlag(a.param("flag"), "true".equalsIgnoreCase(a.param("value")));
                case "flag.applyAll" -> applyFlagAll(a.param("flag"), "true".equalsIgnoreCase(a.param("value")));
                default -> ActionResult.error("Nieznana akcja: " + a.name());
            };
        } catch (Exception e) {
            return ActionResult.error("Blad: " + e.getMessage());
        }
    }

    private ActionResult deleteChunks(String world, String chunksCsv) {
        int[][] chunks = parseChunks(chunksCsv);
        int n = 0;
        for (int[] c : chunks) {
            Optional<Land> land = lands.getLandAt(world, c[0], c[1]);
            lands.unclaimLand(world, c[0], c[1]);
            land.ifPresent(Pl3xMapHook::removeLand);
            n++;
        }
        return ActionResult.ok("Usunieto strefe (" + n + " chunkow).");
    }

    private ActionResult deleteAll(String ownerName) {
        UUID owner = Bukkit.getOfflinePlayer(ownerName).getUniqueId();
        List<Land> toRemove = new ArrayList<>();
        for (Land l : lands.getAllLands()) if (l.getOwner().equals(owner)) toRemove.add(l);
        for (Land l : toRemove) {
            lands.unclaimLand(l.getWorldName(), l.getChunkX(), l.getChunkZ());
            Pl3xMapHook.removeLand(l);
        }
        return ActionResult.ok("Usunieto wszystkie dzialki gracza (" + toRemove.size() + ").");
    }

    private ActionResult setFlag(String world, String chunksCsv, String flag, boolean value) {
        if (flag == null) return ActionResult.error("Brak flagi.");
        int n = 0;
        for (int[] c : parseChunks(chunksCsv)) {
            Land l = lands.getLandAt(world, c[0], c[1]).orElse(null);
            if (l == null) continue;
            l.setFlag(flag, value);
            lands.saveLand(l);
            n++;
        }
        return ActionResult.ok("Flaga " + flag + " = " + value + " (" + n + " chunkow).");
    }

    private ActionResult setTrust(String world, String chunksCsv, String targetName, boolean add) {
        if (targetName == null || targetName.isBlank()) return ActionResult.error("Brak gracza.");
        UUID target = Bukkit.getOfflinePlayer(targetName).getUniqueId();
        int n = 0;
        for (int[] c : parseChunks(chunksCsv)) {
            Land l = lands.getLandAt(world, c[0], c[1]).orElse(null);
            if (l == null) continue;
            if (add) l.addTrusted(target); else l.removeTrusted(target);
            lands.saveLand(l);
            n++;
        }
        return ActionResult.ok((add ? "Dodano" : "Usunieto") + " trusted " + targetName + ".");
    }

    private ActionResult deleteWarp(String ownerName, String name) {
        UUID owner = Bukkit.getOfflinePlayer(ownerName).getUniqueId();
        boolean ok = warps.deleteWarp(owner, name);
        return ok ? ActionResult.ok("Usunieto warp " + name + ".") : ActionResult.error("Nie znaleziono warpa.");
    }

    private ActionResult setLimit(String ownerName, String value) {
        UUID owner = Bukkit.getOfflinePlayer(ownerName).getUniqueId();
        int limit = Integer.parseInt(value.trim());
        lands.setClaimLimitOverride(owner, limit);
        return ActionResult.ok("Limit claimow gracza = " + limit + ".");
    }

    private ActionResult setParticles(boolean value) {
        BellLands.getInstance().getConfig().set("claims.particle-borders", value);
        BellLands.getInstance().saveConfig();
        BellLands.getInstance().reload();
        return ActionResult.ok("Particle borders = " + value + ".");
    }

    private ActionResult setLanguage(String code) {
        if (!"pl".equals(code) && !"en".equals(code)) return ActionResult.error("Jezyk: pl lub en.");
        BellLands.getInstance().getConfig().set("language", code);
        BellLands.getInstance().saveConfig();
        BellLands.getInstance().reload();
        return ActionResult.ok("Jezyk = " + code + ".");
    }

    private ActionResult setDefaultFlag(String flag, boolean value) {
        if (flag == null) return ActionResult.error("Brak flagi.");
        BellLands.getInstance().getConfig().set("claims.default-flags." + flag, value);
        BellLands.getInstance().saveConfig();
        BellLands.getInstance().reload();
        return ActionResult.ok("Domyslna flaga " + flag + " = " + value + ".");
    }

    private ActionResult setLockedFlag(String flag, boolean locked) {
        if (flag == null) return ActionResult.error("Brak flagi.");
        var cfg = BellLands.getInstance().getConfig();
        List<String> list = new ArrayList<>(cfg.getStringList("claims.locked-flags"));
        if (locked) { if (!list.contains(flag)) list.add(flag); }
        else list.remove(flag);
        cfg.set("claims.locked-flags", list);
        BellLands.getInstance().saveConfig();
        BellLands.getInstance().reload();
        return ActionResult.ok("Flaga " + flag + (locked ? " zablokowana." : " odblokowana."));
    }

    private ActionResult applyFlagAll(String flag, boolean value) {
        if (flag == null) return ActionResult.error("Brak flagi.");
        // 1) istniejace dzialki
        lands.applyFlagToAllClaims(flag, value);
        // 2) ORAZ domyslna flaga dla nowych dzialek
        BellLands.getInstance().getConfig().set("claims.default-flags." + flag, value);
        BellLands.getInstance().saveConfig();
        BellLands.getInstance().reload();
        return ActionResult.ok("Flaga " + flag + " = " + value + " na WSZYSTKICH dzialkach + domyslna dla nowych.");
    }

    // ── helpery ─────────────────────────────────────────────

    private static int[][] parseChunks(String csv) {
        if (csv == null || csv.isBlank()) return new int[0][];
        String[] parts = csv.split(";");
        List<int[]> out = new ArrayList<>();
        for (String p : parts) {
            String[] xz = p.split(",");
            if (xz.length == 2) out.add(new int[]{Integer.parseInt(xz[0].trim()), Integer.parseInt(xz[1].trim())});
        }
        return out.toArray(new int[0][]);
    }

    private static String chunksCsv(int[][] chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(chunks[i][0]).append(',').append(chunks[i][1]);
        }
        return sb.toString();
    }

    private static String flagsJson(Land land) {
        if (land == null) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : land.getFlags().entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(esc(e.getKey())).append("\":").append(e.getValue());
            first = false;
        }
        return sb.append('}').toString();
    }

    private static String trustedJson(Land land) {
        if (land == null) return "[]";
        List<String> names = new ArrayList<>();
        for (UUID u : land.getTrusted()) {
            String n = Bukkit.getOfflinePlayer(u).getName();
            names.add("\"" + esc(n != null ? n : u.toString().substring(0, 8)) + "\"");
        }
        return arr(names);
    }

    private static String arr(List<String> items) {
        return "[" + String.join(",", items) + "]";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
