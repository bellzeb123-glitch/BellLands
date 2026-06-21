package pl.bell.lands.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.WarpManager;
import pl.bell.lands.model.Land;

import java.util.*;

public class ClaimGui {

    // ── MAIN MENU ───────────────────────────────────────────
    public static void openMainMenu(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Inventory inv = Bukkit.createInventory(null, 45,
            lang.colorize(lang.getRaw("gui-main-title")));

        // Row 1 center: land info
        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = lang.getRaw("info-unknown-owner");

        inv.setItem(4, item(Material.OAK_SIGN,
            lang.getRaw("gui-info-name"),
            lang.getRaw("gui-info-owner", "owner", ownerName),
            lang.getRaw("gui-info-chunk", "x", land.getChunkX(), "z", land.getChunkZ()),
            lang.getRaw("gui-info-world", "world", land.getWorldName()),
            lang.getRaw("gui-info-trusted-count", "count", land.getTrusted().size())
        ));

        // Row 3: navigation buttons
        inv.setItem(20, item(Material.REDSTONE_TORCH,
            lang.getRaw("gui-nav-flags"), lang.getRaw("gui-nav-flags-lore")));
        inv.setItem(22, item(Material.PLAYER_HEAD,
            lang.getRaw("gui-nav-members"), lang.getRaw("gui-nav-members-lore")));
        inv.setItem(24, item(Material.FILLED_MAP,
            lang.getRaw("gui-nav-map"), lang.getRaw("gui-nav-map-lore")));

        // Row 4: warps
        inv.setItem(31, item(Material.ENDER_PEARL,
            lang.getRaw("gui-nav-warps"), lang.getRaw("gui-nav-warps-lore")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.MAIN_MENU, inv);
    }

    // ── FLAGS PAGE ──────────────────────────────────────────
    public static final Material[] FLAG_MATERIALS = {
        Material.IRON_SWORD,        // pvp
        Material.TNT,               // explosions
        Material.FIRE_CHARGE,       // explosion-damage
        Material.FLINT_AND_STEEL,   // fire-spread
        Material.ZOMBIE_HEAD,       // mob-spawning
        Material.SHIELD,            // mob-damage
        Material.LAVA_BUCKET,       // lava-flow
        Material.MAGMA_BLOCK,       // lava-damage
        Material.WATER_BUCKET,      // water-flow
        Material.PISTON,            // piston
        Material.OAK_LEAVES         // leaf-decay
    };

    public static final Material[] GUEST_MATERIALS = {
        Material.OAK_DOOR,          // guest-doors
        Material.LEVER,             // guest-use
        Material.CHEST,             // guest-chest
        Material.ITEM_FRAME,        // guest-frames
        Material.LEAD               // guest-animals
    };

    public static boolean isFlagLocked(String flag) {
        List<String> locked = BellLands.getInstance().getConfig().getStringList("claims.locked-flags");
        return locked.contains(flag);
    }

    public static void openFlags(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("gui-flags-title")));

        String[] allFlags = Land.ALL_FLAGS;
        for (int i = 0; i < allFlags.length; i++) {
            String flag = allFlags[i];
            boolean value = land.getFlag(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");
            boolean locked = isFlagLocked(flag);
            String lore = locked ? lang.getRaw("gui-flag-locked") : lang.getRaw("gui-flag-lore");

            inv.setItem(10 + i + (i >= 7 ? 2 : 0), item(FLAG_MATERIALS[i], flagName, status, lore));
        }

        // Guest flags button (row 4)
        inv.setItem(40, item(Material.ARMOR_STAND,
            lang.getRaw("gui-nav-guest"), lang.getRaw("gui-nav-guest-lore")));

        inv.setItem(45, item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.FLAGS, inv);
    }

    // Helper: get flag index from slot in flags page
    public static int getFlagIndexFromSlot(int slot) {
        // Row 2: slots 10-16 = flags 0-6
        if (slot >= 10 && slot <= 16) return slot - 10;
        // Row 3: slots 19-24 = flags 7-12
        if (slot >= 19 && slot <= 24) return slot - 19 + 7;
        return -1;
    }

    // ── GUEST FLAGS PAGE ────────────────────────────────────
    public static void openGuestFlags(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Inventory inv = Bukkit.createInventory(null, 27,
            lang.colorize(lang.getRaw("gui-guest-title")));

        String[] guestFlags = Land.GUEST_FLAGS;
        for (int i = 0; i < guestFlags.length; i++) {
            String flag = guestFlags[i];
            boolean value = land.getFlag(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");
            boolean locked = isFlagLocked(flag);
            String lore = locked ? lang.getRaw("gui-flag-locked") : lang.getRaw("gui-flag-lore");

            inv.setItem(11 + i, item(GUEST_MATERIALS[i], flagName, status, lore));
        }

        inv.setItem(18, item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.GUEST_FLAGS, inv);
    }

    public static final Material[] HOME_MATERIALS = {
        Material.RED_BED, Material.WHITE_BED, Material.LIGHT_BLUE_BED, Material.LIME_BED
    };

    public static void openHomeFlags(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();
        Inventory inv = Bukkit.createInventory(null, 27,
            lang.colorize(lang.getRaw("gui-home-title")));

        for (int i = 0; i < Land.HOME_FLAGS.length; i++) {
            String flag = Land.HOME_FLAGS[i];
            boolean value = land.getFlag(flag);
            String flagName = lang.getRaw("gui-flag-" + flag);
            String status = value ? lang.getRaw("gui-flag-on") : lang.getRaw("gui-flag-off");
            boolean locked = isFlagLocked(flag);
            String lore = locked ? lang.getRaw("gui-flag-locked") : lang.getRaw("gui-flag-lore");
            inv.setItem(11 + i, item(HOME_MATERIALS[i], flagName, status, lore));
        }

        inv.setItem(18, item(Material.ARROW, lang.getRaw("gui-back")));
        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.HOME_FLAGS, inv);
    }

    // ── MEMBERS PAGE ────────────────────────────────────────
    public static void openMembers(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        Inventory inv = Bukkit.createInventory(null, 45,
            lang.colorize(lang.getRaw("gui-members-title")));

        // Row 1: add trusted button
        inv.setItem(4, item(Material.LIME_WOOL,
            lang.getRaw("gui-add-trusted-name"), lang.getRaw("gui-add-trusted-lore")));

        // Row 2-4: current trusted as heads
        int slot = 18;
        for (UUID uuid : land.getTrusted()) {
            if (slot > 35) break;
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            if (name == null) name = uuid.toString().substring(0, 8);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.displayName(lang.colorize("&e" + name));
            meta.lore(List.of(lang.colorize(lang.getRaw("gui-remove-trusted-lore"))));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        if (land.getTrusted().isEmpty()) {
            inv.setItem(22, item(Material.BARRIER,
                lang.getRaw("gui-trusted-lore-empty")));
        }

        inv.setItem(36, item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.MEMBERS, inv);
    }

    // ── ADD TRUSTED PAGE ────────────────────────────────────
    public static void openAddTrusted(Player player, Land land) {
        LangManager lang = BellLands.getInstance().getLangManager();

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);
        online.removeIf(p -> land.isTrusted(p.getUniqueId()));

        int size = Math.max(9, ((online.size() + 1) / 9 + 1) * 9);
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size,
            lang.colorize(lang.getRaw("gui-add-trusted-title")));

        int slot = 0;
        for (Player p : online) {
            if (slot >= size - 1) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(p);
            meta.displayName(lang.colorize("&a" + p.getName()));
            meta.lore(List.of(lang.colorize(lang.getRaw("gui-add-trusted-lore"))));
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        inv.setItem(size - 1, item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.ADD_TRUSTED, inv);
    }

    // ── WARPS PAGE ──────────────────────────────────────────
    public static void openWarps(Player player) {
        LangManager lang = BellLands.getInstance().getLangManager();
        WarpManager warpManager = BellLands.getInstance().getWarpManager();

        Inventory inv = Bukkit.createInventory(null, 45,
            lang.colorize(lang.getRaw("gui-warps-title")));

        Set<String> names = warpManager.getWarpNames(player.getUniqueId());
        int slot = 10;
        for (String name : names) {
            if (slot > 34) break;
            inv.setItem(slot, item(Material.ENDER_PEARL,
                "&b" + name,
                lang.getRaw("gui-warp-click-tp"),
                lang.getRaw("gui-warp-shift-delete")));
            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        if (names.isEmpty()) {
            inv.setItem(22, item(Material.BARRIER, lang.getRaw("warp-none")));
        }

        inv.setItem(36, item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.WARPS, inv);
    }

    // ── MAP PAGE (7x5 chunk grid) ───────────────────────────
    public static void openMap(Player player) {
        LangManager lang = BellLands.getInstance().getLangManager();
        LandManager landManager = BellLands.getInstance().getLandManager();

        Inventory inv = Bukkit.createInventory(null, 54,
            lang.colorize(lang.getRaw("gui-map-title")));

        int playerCX = player.getLocation().getChunk().getX();
        int playerCZ = player.getLocation().getChunk().getZ();
        String worldName = player.getWorld().getName();

        // 7 wide (cols 1-7) x 5 tall (rows 0-4) = 35 chunk cells
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                int cx = playerCX + dx;
                int cz = playerCZ + dz;
                int row = dz + 2;
                int col = dx + 4;
                int slot = row * 9 + col;

                org.bukkit.Chunk chunk = player.getWorld().getChunkAt(cx, cz);
                Optional<Land> opt = landManager.getLandAt(chunk);

                Material mat;
                String name;
                String lore;

                String coords = "&7" + cx + ", " + cz;
                if (dx == 0 && dz == 0) {
                    if (opt.isPresent() && opt.get().getOwner().equals(player.getUniqueId())) {
                        mat = Material.LIME_STAINED_GLASS_PANE;
                        name = lang.getRaw("gui-map-here-own");
                        lore = coords;
                    } else if (opt.isPresent()) {
                        String owner = Bukkit.getOfflinePlayer(opt.get().getOwner()).getName();
                        mat = Material.RED_STAINED_GLASS_PANE;
                        name = lang.getRaw("gui-map-here-other", "owner", owner != null ? owner : "?");
                        lore = coords;
                    } else {
                        mat = Material.YELLOW_STAINED_GLASS_PANE;
                        name = lang.getRaw("gui-map-here-free");
                        lore = coords + "\n" + lang.getRaw("gui-map-click-claim");
                    }
                } else if (opt.isPresent()) {
                    Land land = opt.get();
                    if (land.getOwner().equals(player.getUniqueId())) {
                        mat = Material.LIME_STAINED_GLASS_PANE;
                        name = lang.getRaw("gui-map-own");
                        lore = coords + "\n" + lang.getRaw("gui-map-click-unclaim");
                    } else {
                        String owner = Bukkit.getOfflinePlayer(land.getOwner()).getName();
                        mat = Material.RED_STAINED_GLASS_PANE;
                        name = lang.getRaw("gui-map-other", "owner", owner != null ? owner : "?");
                        lore = coords;
                    }
                } else {
                    mat = Material.GRAY_STAINED_GLASS_PANE;
                    name = lang.getRaw("gui-map-free");
                    lore = coords + "\n" + lang.getRaw("gui-map-click-claim");
                }

                inv.setItem(slot, item(mat, name, lore.split("\n")));
            }
        }

        inv.setItem(49, item(Material.ARROW, lang.getRaw("gui-back")));

        ClaimGuiListener.openGui(player, ClaimGuiListener.GuiType.MAP, inv);
    }

    // ── HELPER ──────────────────────────────────────────────
    static ItemStack item(Material material, String name, String... lore) {
        LangManager lang = BellLands.getInstance().getLangManager();
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(lang.colorize(name));
        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(lang.colorize(line));
            }
            meta.lore(loreComponents);
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
