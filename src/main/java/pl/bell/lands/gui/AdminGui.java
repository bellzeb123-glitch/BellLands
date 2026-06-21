package pl.bell.lands.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.WarpManager;
import pl.bell.lands.model.Land;

import java.util.*;
import java.util.stream.Collectors;

public class AdminGui {

    // ── MAIN: players list + settings button ────────────────
    public static void openMain(Player admin) {
        LangManager lang = BellLands.getInstance().getLangManager();
        LandManager landManager = BellLands.getInstance().getLandManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();

        Set<UUID> owners = new LinkedHashSet<>();
        for (Land land : landManager.getAllLands()) {
            owners.add(land.getOwner());
        }
        owners.addAll(warpManager.getAllWarpOwners());

        List<UUID> sortedOwners = new ArrayList<>(owners);
        sortedOwners.sort(Comparator.comparing(uuid -> {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name != null ? name.toLowerCase(Locale.ROOT) : uuid.toString();
        }));

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("admin-main-title")));

        int slot = 0;
        for (UUID uuid : sortedOwners) {
            if (slot >= 44) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            int claimCount = landManager.getClaimCount(uuid);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(lang.colorize("&e" + name));
            meta.lore(List.of(
                lang.colorize(lang.getRaw("admin-player-claims", "count", claimCount)),
                lang.colorize(warpLimitLine(lang, warpManager, uuid)),
                lang.colorize(lang.getRaw("admin-player-click")),
                lang.colorize(lang.getRaw("admin-player-shift-delete"))
            ));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        if (sortedOwners.isEmpty()) {
            inv.setItem(22, ClaimGui.item(Material.BARRIER, lang.getRaw("admin-no-claims")));
        }

        // Bottom row: settings
        inv.setItem(49, ClaimGui.item(Material.COMPARATOR,
            lang.getRaw("admin-settings"), lang.getRaw("admin-settings-lore")));

        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_MAIN, inv);
    }

    // ── PLAYER CLAIMS: individual + zone view ───────────────
    public static void openPlayerClaims(Player admin, UUID targetOwner) {
        LangManager lang = BellLands.getInstance().getLangManager();
        LandManager landManager = BellLands.getInstance().getLandManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();

        List<Land> playerLands = landManager.getAllLands().stream()
            .filter(l -> l.getOwner().equals(targetOwner))
            .collect(Collectors.toList());

        String ownerName = Bukkit.getOfflinePlayer(targetOwner).getName();
        if (ownerName == null) ownerName = targetOwner.toString().substring(0, 8);

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("admin-player-title", "player", ownerName)));

        // Group into connected zones
        List<List<Land>> zones = groupIntoZones(playerLands);

        // Row 0: zone buttons
        for (int i = 0; i < Math.min(zones.size(), 9); i++) {
            List<Land> zone = zones.get(i);
            int[] bounds = getZoneBounds(zone);
            inv.setItem(i, ClaimGui.item(Material.FILLED_MAP,
                lang.getRaw("admin-zone", "id", i + 1, "count", zone.size()),
                "&7" + bounds[0] + "," + bounds[1] + " -> " + bounds[2] + "," + bounds[3],
                lang.getRaw("admin-zone-click"),
                lang.getRaw("admin-zone-shift-delete")
            ));
        }

        // Row 2-5: individual claims
        int slot = 18;
        for (Land land : playerLands) {
            if (slot > 52) break;
            inv.setItem(slot++, ClaimGui.item(Material.GRASS_BLOCK,
                "&a" + land.getWorldName() + " &7[" + land.getChunkX() + ", " + land.getChunkZ() + "]",
                lang.getRaw("admin-claim-trusted", "count", land.getTrusted().size()),
                lang.getRaw("admin-claim-click-manage"),
                lang.getRaw("admin-claim-shift-delete")
            ));
        }

        // Bottom
        inv.setItem(45, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));
        int warpCount = warpManager.getWarpCount(targetOwner);
        int warpMax = warpManager.getMaxWarpsForOwner(targetOwner);
        String warpButtonLore = warpManager.isUnlimitedWarps(warpMax)
            ? lang.getRaw("admin-warps-button-lore-unlimited", "count", warpCount)
            : lang.getRaw("admin-warps-button-lore", "count", warpCount, "max", warpMax);
        inv.setItem(47, ClaimGui.item(Material.ENDER_PEARL,
            lang.getRaw("admin-warps-button"), warpButtonLore, lang.getRaw("admin-view-warps-lore")));
        inv.setItem(53, ClaimGui.item(Material.TNT,
            lang.getRaw("admin-delete-all"), lang.getRaw("admin-delete-all-lore")));

        AdminGuiContext.set(admin.getUniqueId(), targetOwner, playerLands);
        AdminGuiContext.setZones(admin.getUniqueId(), zones);
        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_PLAYER_CLAIMS, inv);
    }

    // ── CLAIM DETAIL ────────────────────────────────────────
    public static void openClaimDetail(Player admin, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("admin-claim-title",
                "x", land.getChunkX(), "z", land.getChunkZ())));

        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = "?";

        inv.setItem(4, ClaimGui.item(Material.OAK_SIGN,
            lang.getRaw("gui-info-name"),
            lang.getRaw("gui-info-owner", "owner", ownerName),
            lang.getRaw("gui-info-chunk", "x", land.getChunkX(), "z", land.getChunkZ()),
            lang.getRaw("gui-info-world", "world", land.getWorldName())
        ));

        // Row 1-2: all flags (protection + guest)
        String[] allFlags = Land.ALL_FLAGS;
        for (int i = 0; i < allFlags.length; i++) {
            String flag = allFlags[i];
            boolean value = land.getFlag(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");
            boolean locked = ClaimGui.isFlagLocked(flag);
            String lockInfo = locked ? " &4[LOCKED]" : "";
            inv.setItem(9 + i + (i >= 7 ? 2 : 0),
                ClaimGui.item(ClaimGui.FLAG_MATERIALS[i], flagName, status + lockInfo, lang.getRaw("gui-flag-lore")));
        }

        // Guest flags (row 3)
        String[] guestFlags = Land.GUEST_FLAGS;
        for (int i = 0; i < guestFlags.length; i++) {
            String flag = guestFlags[i];
            boolean value = land.getFlag(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");
            inv.setItem(27 + i, ClaimGui.item(ClaimGui.GUEST_MATERIALS[i], flagName, status, lang.getRaw("gui-flag-lore")));
        }

        // Row 4: trusted
        int slot = 36;
        for (UUID uuid : land.getTrusted()) {
            if (slot > 43) break;
            OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
            String name = op.getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(op);
            meta.displayName(lang.colorize("&e" + name));
            meta.lore(List.of(lang.colorize(lang.getRaw("admin-trusted-shift-remove"))));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        // Bottom
        inv.setItem(45, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));
        inv.setItem(49, ClaimGui.item(Material.ENDER_PEARL,
            lang.getRaw("admin-view-warps"), lang.getRaw("admin-view-warps-lore")));
        inv.setItem(53, ClaimGui.item(Material.TNT,
            lang.getRaw("admin-delete-claim"), lang.getRaw("admin-delete-claim-lore")));

        AdminGuiContext.setCurrentLand(admin.getUniqueId(), land);
        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_CLAIM_DETAIL, inv);
    }

    // ── PLAYER WARPS ────────────────────────────────────────
    public static void openPlayerWarps(Player admin, UUID targetOwner) {
        LangManager lang = BellLands.getInstance().getLangManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();

        Map<String, Location> playerWarps = new LinkedHashMap<>();
        for (String name : warpManager.getWarpNames(targetOwner)) {
            Location loc = warpManager.getWarp(targetOwner, name);
            if (loc != null) {
                playerWarps.put(name, loc);
            }
        }

        String ownerName = Bukkit.getOfflinePlayer(targetOwner).getName();
        if (ownerName == null) ownerName = "?";

        int warpCount = playerWarps.size();
        int warpMax = warpManager.getMaxWarpsForOwner(targetOwner);
        String limitLine = warpManager.isUnlimitedWarps(warpMax)
            ? lang.getRaw("admin-warps-limit-unlimited", "count", warpCount)
            : lang.getRaw("admin-warps-limit", "count", warpCount, "max", warpMax);

        int size = Math.max(27, ((Math.max(warpCount, 1) + 8) / 9 + 1) * 9);
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size,
            lang.colorize(lang.getRaw("admin-warps-title", "player", ownerName)));

        inv.setItem(4, ClaimGui.item(Material.COMPASS,
            lang.getRaw("admin-warps-limit-header"), limitLine));

        int slot = 9;
        for (Map.Entry<String, Location> entry : playerWarps.entrySet()) {
            if (slot >= size - 1) break;
            Location loc = entry.getValue();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "?";
            inv.setItem(slot++, ClaimGui.item(Material.ENDER_PEARL,
                "&b" + entry.getKey(),
                lang.getRaw("admin-warp-location",
                    "world", worldName,
                    "x", (int) Math.floor(loc.getX()),
                    "y", (int) Math.floor(loc.getY()),
                    "z", (int) Math.floor(loc.getZ())),
                lang.getRaw("admin-warp-click-tp"),
                lang.getRaw("admin-warp-shift-delete")));
            if (slot % 9 == 8) slot += 2;
        }

        if (playerWarps.isEmpty()) {
            inv.setItem(22, ClaimGui.item(Material.BARRIER, lang.getRaw("warp-none")));
        }

        inv.setItem(size - 1, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));

        AdminGuiContext.set(admin.getUniqueId(), targetOwner, null);
        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_WARPS, inv);
    }

    // ── SETTINGS PAGE ───────────────────────────────────────
    public static void openSettings(Player admin) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Inventory inv = Bukkit.createInventory(null, 36,
            lang.colorize(lang.getRaw("admin-settings-title")));

        // Particles toggle
        boolean particles = BellLands.getInstance().getConfig().getBoolean("claims.particle-borders", true);
        inv.setItem(10, ClaimGui.item(Material.BLAZE_POWDER,
            lang.getRaw("admin-particles-toggle"),
            particles ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off"),
            lang.getRaw("gui-flag-lore")));

        // Language toggle
        String currentLang = BellLands.getInstance().getConfig().getString("language", "pl");
        inv.setItem(12, ClaimGui.item(Material.WRITABLE_BOOK,
            lang.getRaw("admin-language-toggle"),
            "&7" + currentLang.toUpperCase(),
            lang.getRaw("admin-language-click")));

        // Locked flags
        List<String> locked = BellLands.getInstance().getConfig().getStringList("claims.locked-flags");
        inv.setItem(14, ClaimGui.item(Material.IRON_BARS,
            lang.getRaw("admin-locked-flags"),
            lang.getRaw("admin-locked-count", "count", locked.size()),
            lang.getRaw("admin-locked-click")));

        // Default flags
        inv.setItem(16, ClaimGui.item(Material.COMMAND_BLOCK,
            lang.getRaw("admin-defaults"),
            lang.getRaw("admin-defaults-lore")));

        inv.setItem(27, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_SETTINGS, inv);
    }

    // ── LOCKED FLAGS PAGE ────────────────────────────────────
    public static void openLockedFlags(Player admin) {
        LangManager lang = BellLands.getInstance().getLangManager();
        List<String> locked = BellLands.getInstance().getConfig().getStringList("claims.locked-flags");

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("admin-locked-title")));

        // Protection flags: same layout as flags page
        String[] allFlags = Land.ALL_FLAGS;
        for (int i = 0; i < allFlags.length; i++) {
            String flag = allFlags[i];
            boolean isLocked = locked.contains(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = isLocked ? lang.getRaw("admin-lock-on") : lang.getRaw("admin-lock-off");

            Material mat = isLocked ? Material.RED_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE;
            inv.setItem(10 + i + (i >= 7 ? 2 : 0), ClaimGui.item(mat, flagName, status, lang.getRaw("gui-flag-lore")));
        }

        // Guest flags label
        inv.setItem(27, ClaimGui.item(Material.ARMOR_STAND,
            lang.getRaw("gui-nav-guest")));

        // Guest flags: slots 28, 29, 30
        String[] guestFlags = Land.GUEST_FLAGS;
        for (int i = 0; i < guestFlags.length; i++) {
            String flag = guestFlags[i];
            boolean isLocked = locked.contains(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = isLocked ? lang.getRaw("admin-lock-on") : lang.getRaw("admin-lock-off");

            Material mat = isLocked ? Material.RED_STAINED_GLASS_PANE : Material.GREEN_STAINED_GLASS_PANE;
            inv.setItem(28 + i, ClaimGui.item(mat, flagName, status, lang.getRaw("gui-flag-lore")));
        }

        inv.setItem(45, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_LOCKED_FLAGS, inv);
    }

    // ── DEFAULTS PAGE ─────────────────────────────────────────
    public static void openDefaults(Player admin) {
        LangManager lang = BellLands.getInstance().getLangManager();
        var config = BellLands.getInstance().getConfig();

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("admin-defaults-title")));

        String[] allFlags = Land.ALL_FLAGS;
        for (int i = 0; i < allFlags.length; i++) {
            String flag = allFlags[i];
            boolean value = config.getBoolean("claims.default-flags." + flag, false);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");

            inv.setItem(10 + i + (i >= 7 ? 2 : 0),
                ClaimGui.item(ClaimGui.FLAG_MATERIALS[i], flagName, status, lang.getRaw("gui-flag-lore")));
        }

        inv.setItem(40, ClaimGui.item(Material.ARMOR_STAND,
            lang.getRaw("gui-nav-guest"), lang.getRaw("gui-nav-guest-lore")));
        inv.setItem(45, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_DEFAULTS, inv);
    }

    public static void openDefaultsGuest(Player admin) {
        LangManager lang = BellLands.getInstance().getLangManager();
        var config = BellLands.getInstance().getConfig();

        Inventory inv = Bukkit.createInventory(null, 27,
            lang.colorize(lang.getRaw("admin-defaults-guest-title")));

        String[] guestFlags = Land.GUEST_FLAGS;
        for (int i = 0; i < guestFlags.length; i++) {
            String flag = guestFlags[i];
            boolean value = config.getBoolean("claims.default-flags." + flag, false);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");

            inv.setItem(11 + i, ClaimGui.item(ClaimGui.GUEST_MATERIALS[i], flagName, status, lang.getRaw("gui-flag-lore")));
        }

        inv.setItem(18, ClaimGui.item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(admin, ClaimGuiListener.GuiType.ADMIN_DEFAULTS_GUEST, inv);
    }

    // ── Zone grouping ───────────────────────────────────────
    public static List<List<Land>> groupIntoZones(List<Land> lands) {
        List<List<Land>> zones = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        Map<String, Land> landMap = new HashMap<>();
        for (Land l : lands) {
            landMap.put(l.getWorldName() + ";" + l.getChunkX() + ";" + l.getChunkZ(), l);
        }

        for (Land l : lands) {
            String key = l.getWorldName() + ";" + l.getChunkX() + ";" + l.getChunkZ();
            if (visited.contains(key)) continue;

            List<Land> zone = new ArrayList<>();
            Queue<Land> queue = new LinkedList<>();
            queue.add(l);
            visited.add(key);

            while (!queue.isEmpty()) {
                Land current = queue.poll();
                zone.add(current);
                int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
                for (int[] d : dirs) {
                    String nk = current.getWorldName() + ";" +
                        (current.getChunkX() + d[0]) + ";" + (current.getChunkZ() + d[1]);
                    if (!visited.contains(nk) && landMap.containsKey(nk)) {
                        visited.add(nk);
                        queue.add(landMap.get(nk));
                    }
                }
            }
            zones.add(zone);
        }
        return zones;
    }

    public static int[] getZoneBounds(List<Land> zone) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (Land l : zone) {
            minX = Math.min(minX, l.getChunkX());
            minZ = Math.min(minZ, l.getChunkZ());
            maxX = Math.max(maxX, l.getChunkX());
            maxZ = Math.max(maxZ, l.getChunkZ());
        }
        return new int[]{minX, minZ, maxX, maxZ};
    }

    private static String warpLimitLine(LangManager lang, WarpManager warpManager, UUID owner) {
        int count = warpManager.getWarpCount(owner);
        int max = warpManager.getMaxWarpsForOwner(owner);
        if (warpManager.isUnlimitedWarps(max)) {
            return lang.getRaw("admin-player-warps-unlimited", "count", count);
        }
        return lang.getRaw("admin-player-warps", "count", count, "max", max);
    }

    // ── Context ─────────────────────────────────────────────
    public static class AdminGuiContext {
        private static final Map<UUID, UUID> targetOwners = new HashMap<>();
        private static final Map<UUID, List<Land>> targetLands = new HashMap<>();
        private static final Map<UUID, Land> currentLand = new HashMap<>();
        private static final Map<UUID, List<List<Land>>> zonesMap = new HashMap<>();

        public static void set(UUID admin, UUID target, List<Land> lands) {
            targetOwners.put(admin, target);
            if (lands != null) targetLands.put(admin, lands);
        }
        public static void setCurrentLand(UUID admin, Land land) { currentLand.put(admin, land); }
        public static void setZones(UUID admin, List<List<Land>> zones) { zonesMap.put(admin, zones); }

        public static UUID getTargetOwner(UUID admin) { return targetOwners.get(admin); }
        public static List<Land> getTargetLands(UUID admin) { return targetLands.get(admin); }
        public static Land getCurrentLand(UUID admin) { return currentLand.get(admin); }
        public static List<List<Land>> getZones(UUID admin) { return zonesMap.get(admin); }

        public static void clear(UUID admin) {
            targetOwners.remove(admin);
            targetLands.remove(admin);
            currentLand.remove(admin);
            zonesMap.remove(admin);
        }
    }
}
