package pl.bell.lands.gui;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.WarpManager;
import pl.bell.lands.integration.Pl3xMapHook;

import java.util.*;

public class ClaimGuiListener implements Listener {

    public enum GuiType { MAIN_MENU, FLAGS, GUEST_FLAGS, MEMBERS, ADD_TRUSTED, WARPS, MAP,
        ADMIN_MAIN, ADMIN_PLAYER_CLAIMS, ADMIN_CLAIM_DETAIL, ADMIN_WARPS,
        ADMIN_SETTINGS, ADMIN_LOCKED_FLAGS, ADMIN_DEFAULTS, ADMIN_DEFAULTS_GUEST }

    private static final Map<UUID, GuiType> openGuis = new HashMap<>();

    public static void markOpen(UUID playerId, GuiType type) {
        openGuis.put(playerId, type);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        GuiType type = openGuis.remove(uuid);
        if (type != null && !isAdminType(type)) {
            AdminGui.AdminGuiContext.clear(uuid);
        }
    }

    private boolean isAdminType(GuiType type) {
        return type == GuiType.ADMIN_MAIN || type == GuiType.ADMIN_PLAYER_CLAIMS
            || type == GuiType.ADMIN_CLAIM_DETAIL || type == GuiType.ADMIN_WARPS
            || type == GuiType.ADMIN_SETTINGS || type == GuiType.ADMIN_LOCKED_FLAGS
            || type == GuiType.ADMIN_DEFAULTS || type == GuiType.ADMIN_DEFAULTS_GUEST;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GuiType type = openGuis.get(player.getUniqueId());
        if (type == null) return;

        event.setCancelled(true);

        switch (type) {
            case MAIN_MENU -> handleMainMenu(player, event);
            case FLAGS -> handleFlags(player, event);
            case GUEST_FLAGS -> handleGuestFlags(player, event);
            case MEMBERS -> handleMembers(player, event);
            case ADD_TRUSTED -> handleAddTrusted(player, event);
            case WARPS -> handleWarps(player, event);
            case MAP -> handleMap(player, event);
            case ADMIN_MAIN -> handleAdminMain(player, event);
            case ADMIN_PLAYER_CLAIMS -> handleAdminPlayerClaims(player, event);
            case ADMIN_CLAIM_DETAIL -> handleAdminClaimDetail(player, event);
            case ADMIN_WARPS -> handleAdminWarps(player, event);
            case ADMIN_SETTINGS -> handleAdminSettings(player, event);
            case ADMIN_LOCKED_FLAGS -> handleAdminLockedFlags(player, event);
            case ADMIN_DEFAULTS -> handleAdminDefaults(player, event);
            case ADMIN_DEFAULTS_GUEST -> handleAdminDefaultsGuest(player, event);
        }
    }

    private void handleMainMenu(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;
        Land land = opt.get();

        switch (slot) {
            case 20 -> // Flags
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openFlags(player, land));
            case 22 -> // Members
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openMembers(player, land));
            case 24 -> // Map
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openMap(player));
            case 31 -> // Warps
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openWarps(player));
        }
    }

    private void handleFlags(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 45) { reopenMainMenu(player); return; }

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) return;

        // Guest flags button
        if (slot == 40) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openGuestFlags(player, land));
            return;
        }

        int flagIndex = ClaimGui.getFlagIndexFromSlot(slot);
        if (flagIndex < 0 || flagIndex >= Land.ALL_FLAGS.length) return;

        String flag = Land.ALL_FLAGS[flagIndex];
        if (ClaimGui.isFlagLocked(flag) && !player.isOp()) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("flag-locked-msg"));
            return;
        }

        land.setFlag(flag, !land.getFlag(flag));
        landManager.saveLand(land);
        Bukkit.getScheduler().runTask(BellLands.getInstance(),
            () -> ClaimGui.openFlags(player, land));
    }

    private void handleGuestFlags(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) return;

        if (slot == 18) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openFlags(player, land));
            return;
        }

        // Guest flag slots: 11, 12, 13
        if (slot >= 11 && slot <= 13) {
            int idx = slot - 11;
            if (idx < Land.GUEST_FLAGS.length) {
                String flag = Land.GUEST_FLAGS[idx];
                if (ClaimGui.isFlagLocked(flag) && !player.isOp()) {
                    player.sendMessage(BellLands.getInstance().getLangManager()
                        .component("flag-locked-msg"));
                    return;
                }
                land.setFlag(flag, !land.getFlag(flag));
                landManager.saveLand(land);
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openGuestFlags(player, land));
            }
        }
    }

    private void handleMembers(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 36) { reopenMainMenu(player); return; }

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;
        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) return;

        if (slot == 4) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openAddTrusted(player, land));
            return;
        }

        // Click on trusted head = remove
        if (slot >= 18 && slot <= 35 && clicked.getType() == Material.PLAYER_HEAD
                && clicked.getItemMeta() instanceof SkullMeta skull && skull.getOwningPlayer() != null) {
            UUID targetUuid = skull.getOwningPlayer().getUniqueId();
            land.removeTrusted(targetUuid);
            landManager.saveLand(land);

            LangManager lang = BellLands.getInstance().getLangManager();
            String name = skull.getOwningPlayer().getName();
            if (name == null) name = targetUuid.toString().substring(0, 8);
            player.sendMessage(lang.component("untrust-success", "player", name));

            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openMembers(player, land));
        }
    }

    private void handleAddTrusted(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.ARROW) {
            Chunk chunk = player.getLocation().getChunk();
            LandManager landManager = BellLands.getInstance().getLandManager();
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isPresent()) {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openMembers(player, opt.get()));
            }
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skull) {
            if (skull.getOwningPlayer() == null) return;
            UUID targetUuid = skull.getOwningPlayer().getUniqueId();

            Chunk chunk = player.getLocation().getChunk();
            LandManager landManager = BellLands.getInstance().getLandManager();
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isEmpty()) return;

            Land land = opt.get();
            if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) return;

            if (!land.isTrusted(targetUuid)) {
                land.addTrusted(targetUuid);
                landManager.saveLand(land);

                LangManager lang = BellLands.getInstance().getLangManager();
                String name = skull.getOwningPlayer().getName();
                if (name == null) name = targetUuid.toString().substring(0, 8);
                player.sendMessage(lang.component("trust-success", "player", name));
            }

            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openMembers(player, opt.get()));
        }
    }

    private void handleWarps(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 36) { reopenMainMenu(player); return; }

        if (clicked.getType() == Material.ENDER_PEARL) {
            String warpName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(clicked.getItemMeta().displayName());

            WarpManager warpManager = BellLands.getInstance().getWarpManager();

            if (event.isShiftClick()) {
                if (warpManager.deleteWarp(player.getUniqueId(), warpName)) {
                    LangManager lang = BellLands.getInstance().getLangManager();
                    player.sendMessage(lang.component("warp-deleted", "name", warpName));
                }
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openWarps(player));
            } else {
                Location loc = warpManager.getWarp(player.getUniqueId(), warpName);
                if (loc != null) {
                    player.closeInventory();
                    player.teleport(loc);
                    LangManager lang = BellLands.getInstance().getLangManager();
                    player.sendMessage(lang.component("warp-teleported", "name", warpName));
                }
            }
        }
    }

    private void handleMap(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 49) { reopenMainMenu(player); return; }

        int playerCX = player.getLocation().getChunk().getX();
        int playerCZ = player.getLocation().getChunk().getZ();

        int row = slot / 9;
        int col = slot % 9;
        if (row < 0 || row > 4 || col < 1 || col > 7) return;

        int dx = col - 4;
        int dz = row - 2;
        int cx = playerCX + dx;
        int cz = playerCZ + dz;

        LandManager landManager = BellLands.getInstance().getLandManager();
        LangManager lang = BellLands.getInstance().getLangManager();
        org.bukkit.Chunk chunk = player.getWorld().getChunkAt(cx, cz);
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent()) {
            Land land = opt.get();
            if (land.getOwner().equals(player.getUniqueId())) {
                landManager.unclaimLand(player.getWorld().getName(), cx, cz);
                Pl3xMapHook.removeLand(land);
                player.sendMessage(lang.component("unclaim-success"));
            }
        } else {
            int current = landManager.getClaimCount(player.getUniqueId());
            int max = landManager.getMaxClaims(player);
            if (current >= max) {
                player.sendMessage(lang.component("claim-limit-reached", "current", current, "max", max));
            } else {
                Land land = new Land(player.getUniqueId(), player.getWorld().getName(), cx, cz);
                landManager.claimLand(land);
                Pl3xMapHook.drawLand(land);
                player.sendMessage(lang.component("claim-success", "x", cx, "z", cz));
            }
        }

        Bukkit.getScheduler().runTask(BellLands.getInstance(),
            () -> ClaimGui.openMap(player));
    }

    // ── Admin GUI handlers ────────────────────────────────

    private void handleAdminMain(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Settings button
        if (slot == 49) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openSettings(player));
            return;
        }

        if (clicked.getType() != Material.PLAYER_HEAD) return;
        if (!(clicked.getItemMeta() instanceof SkullMeta skull) || skull.getOwningPlayer() == null) return;

        UUID targetOwner = skull.getOwningPlayer().getUniqueId();

        if (event.isShiftClick()) {
            // Delete ALL claims of this player
            LandManager landManager = BellLands.getInstance().getLandManager();
            LangManager lang = BellLands.getInstance().getLangManager();
            List<Land> playerLands = landManager.getAllLands().stream()
                .filter(l -> l.getOwner().equals(targetOwner))
                .toList();
            int count = playerLands.size();
            for (Land l : playerLands) {
                landManager.unclaimLand(l.getWorldName(), l.getChunkX(), l.getChunkZ());
            }
            Pl3xMapHook.drawAll();
            String name = skull.getOwningPlayer().getName();
            if (name == null) name = "?";
            player.sendMessage(lang.component("admin-all-deleted", "player", name, "count", count));
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openMain(player));
        } else {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openPlayerClaims(player, targetOwner));
        }
    }

    private void handleAdminPlayerClaims(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID targetOwner = AdminGui.AdminGuiContext.getTargetOwner(player.getUniqueId());
        LandManager landManager = BellLands.getInstance().getLandManager();
        LangManager lang = BellLands.getInstance().getLangManager();

        if (slot == 45) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openMain(player));
            return;
        }

        // Delete all button
        if (slot == 53 && targetOwner != null) {
            List<Land> all = landManager.getAllLands().stream()
                .filter(l -> l.getOwner().equals(targetOwner)).toList();
            int count = all.size();
            for (Land l : all) {
                landManager.unclaimLand(l.getWorldName(), l.getChunkX(), l.getChunkZ());
            }
            Pl3xMapHook.drawAll();
            String name = Bukkit.getOfflinePlayer(targetOwner).getName();
            player.sendMessage(lang.component("admin-all-deleted", "player", name != null ? name : "?", "count", count));
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openMain(player));
            return;
        }

        // Zone buttons (row 0, slots 0-8)
        if (slot >= 0 && slot <= 8 && clicked.getType() == Material.FILLED_MAP) {
            List<List<Land>> zones = AdminGui.AdminGuiContext.getZones(player.getUniqueId());
            if (zones != null && slot < zones.size()) {
                List<Land> zone = zones.get(slot);
                if (event.isShiftClick()) {
                    // Delete entire zone
                    for (Land l : zone) {
                        landManager.unclaimLand(l.getWorldName(), l.getChunkX(), l.getChunkZ());
                    }
                    Pl3xMapHook.drawAll();
                    player.sendMessage(lang.component("admin-zone-deleted", "count", zone.size()));
                    Bukkit.getScheduler().runTask(BellLands.getInstance(),
                        () -> AdminGui.openPlayerClaims(player, targetOwner));
                } else {
                    // Show first claim detail of zone (manage zone flags)
                    if (!zone.isEmpty()) {
                        Bukkit.getScheduler().runTask(BellLands.getInstance(),
                            () -> AdminGui.openClaimDetail(player, zone.get(0)));
                    }
                }
            }
            return;
        }

        // Individual claims (row 2+, slot 18+)
        if (slot >= 18 && slot <= 52) {
            List<Land> lands = AdminGui.AdminGuiContext.getTargetLands(player.getUniqueId());
            int landIdx = slot - 18;
            if (lands == null || landIdx < 0 || landIdx >= lands.size()) return;
            Land land = lands.get(landIdx);

            if (event.isShiftClick()) {
                landManager.unclaimLand(land.getWorldName(), land.getChunkX(), land.getChunkZ());
                Pl3xMapHook.drawAll();
                player.sendMessage(lang.component("admin-claim-deleted",
                    "x", land.getChunkX(), "z", land.getChunkZ()));
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openPlayerClaims(player, targetOwner));
            } else {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openClaimDetail(player, land));
            }
        }
    }

    private void handleAdminClaimDetail(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Land land = AdminGui.AdminGuiContext.getCurrentLand(player.getUniqueId());
        if (land == null) return;
        LandManager landManager = BellLands.getInstance().getLandManager();
        LangManager lang = BellLands.getInstance().getLangManager();

        if (slot == 45) {
            UUID targetOwner = AdminGui.AdminGuiContext.getTargetOwner(player.getUniqueId());
            if (targetOwner != null) {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openPlayerClaims(player, targetOwner));
            }
            return;
        }

        if (slot == 49) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openPlayerWarps(player, land.getOwner()));
            return;
        }

        if (slot == 53) {
            landManager.unclaimLand(land.getWorldName(), land.getChunkX(), land.getChunkZ());
            Pl3xMapHook.drawAll();
            player.sendMessage(lang.component("admin-claim-deleted",
                "x", land.getChunkX(), "z", land.getChunkZ()));
            UUID targetOwner = AdminGui.AdminGuiContext.getTargetOwner(player.getUniqueId());
            if (targetOwner != null) {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openPlayerClaims(player, targetOwner));
            }
            return;
        }

        // Protection flag toggle (row 1-2, slots 9-15 and 18-22)
        int flagIndex = -1;
        if (slot >= 9 && slot <= 15) flagIndex = slot - 9;
        else if (slot >= 18 && slot <= 22) flagIndex = slot - 18 + 7;
        // slot 16 is gap, 23/24 continue: 18+7=25? Let me recalculate
        // ALL_FLAGS has 13 items. Row layout: 9..15 = 0..6, then 18..24 = 7..12 (with gap at 16,17)
        if (slot >= 18 && slot <= 24) flagIndex = slot - 18 + 7;

        if (flagIndex >= 0 && flagIndex < Land.ALL_FLAGS.length) {
            String flag = Land.ALL_FLAGS[flagIndex];
            land.setFlag(flag, !land.getFlag(flag));
            landManager.saveLand(land);
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openClaimDetail(player, land));
            return;
        }

        // Guest flags (row 3: slots 27, 28, 29)
        if (slot >= 27 && slot <= 29) {
            int gIdx = slot - 27;
            if (gIdx < Land.GUEST_FLAGS.length) {
                String flag = Land.GUEST_FLAGS[gIdx];
                land.setFlag(flag, !land.getFlag(flag));
                landManager.saveLand(land);
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openClaimDetail(player, land));
            }
            return;
        }

        // Remove trusted
        if (slot >= 36 && slot <= 43 && event.isShiftClick()
                && clicked.getType() == Material.PLAYER_HEAD
                && clicked.getItemMeta() instanceof SkullMeta skull
                && skull.getOwningPlayer() != null) {
            UUID targetUuid = skull.getOwningPlayer().getUniqueId();
            land.removeTrusted(targetUuid);
            landManager.saveLand(land);
            String name = skull.getOwningPlayer().getName();
            if (name == null) name = targetUuid.toString().substring(0, 8);
            player.sendMessage(lang.component("untrust-success", "player", name));
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openClaimDetail(player, land));
        }
    }

    private void handleAdminWarps(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID targetOwner = AdminGui.AdminGuiContext.getTargetOwner(player.getUniqueId());

        if (clicked.getType() == Material.ARROW) {
            if (targetOwner != null) {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openPlayerClaims(player, targetOwner));
            }
            return;
        }

        if (clicked.getType() == Material.ENDER_PEARL && targetOwner != null) {
            String warpName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(clicked.getItemMeta().displayName());

            WarpManager warpManager = BellLands.getInstance().getWarpManager();

            if (event.isShiftClick()) {
                if (warpManager.deleteWarp(targetOwner, warpName)) {
                    LangManager lang = BellLands.getInstance().getLangManager();
                    player.sendMessage(lang.component("admin-warp-deleted", "name", warpName));
                }
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openPlayerWarps(player, targetOwner));
            } else {
                org.bukkit.Location loc = warpManager.getWarp(targetOwner, warpName);
                if (loc != null) {
                    player.closeInventory();
                    player.teleport(loc);
                    LangManager lang = BellLands.getInstance().getLangManager();
                    player.sendMessage(lang.component("warp-teleported", "name", warpName));
                }
            }
        }
    }

    private void handleAdminSettings(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 27) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openMain(player));
            return;
        }

        BellLands plugin = BellLands.getInstance();
        LangManager lang = plugin.getLangManager();

        // Toggle particles
        if (slot == 10) {
            boolean current = plugin.getConfig().getBoolean("claims.particle-borders", true);
            plugin.getConfig().set("claims.particle-borders", !current);
            plugin.saveConfig();
            player.sendMessage(lang.component(!current ? "particles-on" : "particles-off"));
            Bukkit.getScheduler().runTask(plugin, () -> AdminGui.openSettings(player));
            return;
        }

        // Toggle language
        if (slot == 12) {
            String current = plugin.getConfig().getString("language", "pl");
            String next = current.equals("pl") ? "en" : "pl";
            plugin.getConfig().set("language", next);
            plugin.saveConfig();
            plugin.reload();
            player.sendMessage(lang.component("language-changed", "lang", next));
            Bukkit.getScheduler().runTask(plugin, () -> AdminGui.openSettings(player));
            return;
        }

        // Open locked flags page
        if (slot == 14) {
            Bukkit.getScheduler().runTask(plugin, () -> AdminGui.openLockedFlags(player));
            return;
        }

        // Open defaults page
        if (slot == 16) {
            Bukkit.getScheduler().runTask(plugin, () -> AdminGui.openDefaults(player));
        }
    }

    private void handleAdminLockedFlags(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Back
        if (slot == 45) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openSettings(player));
            return;
        }

        // Protection flags: slots 10-16 = 0-6, slots 19-24 = 7-12
        int flagIndex = -1;
        if (slot >= 10 && slot <= 16) flagIndex = slot - 10;
        else if (slot >= 19 && slot <= 24) flagIndex = slot - 19 + 7;

        if (flagIndex >= 0 && flagIndex < Land.ALL_FLAGS.length) {
            toggleLockedFlag(player, Land.ALL_FLAGS[flagIndex]);
            return;
        }

        // Guest flags: slots 28, 29, 30
        if (slot >= 28 && slot <= 30) {
            int gIdx = slot - 28;
            if (gIdx < Land.GUEST_FLAGS.length) {
                toggleLockedFlag(player, Land.GUEST_FLAGS[gIdx]);
            }
        }
    }

    private void handleAdminDefaults(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 45) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openSettings(player));
            return;
        }

        if (slot == 40) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openDefaultsGuest(player));
            return;
        }

        int flagIndex = -1;
        if (slot >= 10 && slot <= 16) flagIndex = slot - 10;
        else if (slot >= 19 && slot <= 24) flagIndex = slot - 19 + 7;

        if (flagIndex >= 0 && flagIndex < Land.ALL_FLAGS.length) {
            String flag = Land.ALL_FLAGS[flagIndex];
            toggleDefaultFlag(player, flag);
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openDefaults(player));
        }
    }

    private void handleAdminDefaultsGuest(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == 18) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> AdminGui.openDefaults(player));
            return;
        }

        if (slot >= 11 && slot <= 13) {
            int gIdx = slot - 11;
            if (gIdx < Land.GUEST_FLAGS.length) {
                String flag = Land.GUEST_FLAGS[gIdx];
                toggleDefaultFlag(player, flag);
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> AdminGui.openDefaultsGuest(player));
            }
        }
    }

    private void toggleDefaultFlag(Player player, String flag) {
        BellLands plugin = BellLands.getInstance();
        String path = "claims.default-flags." + flag;
        boolean current = plugin.getConfig().getBoolean(path, false);
        plugin.getConfig().set(path, !current);
        plugin.saveConfig();
        String status = !current
            ? plugin.getLangManager().getRaw("gui-flag-on")
            : plugin.getLangManager().getRaw("gui-flag-off");
        player.sendMessage(plugin.getLangManager().component("admin-default-changed", "flag", flag, "value", status));
    }

    private void toggleLockedFlag(Player player, String flag) {
        BellLands plugin = BellLands.getInstance();
        List<String> locked = new ArrayList<>(plugin.getConfig().getStringList("claims.locked-flags"));
        if (locked.contains(flag)) {
            locked.remove(flag);
            player.sendMessage(plugin.getLangManager().component("admin-flag-unlocked", "flag", flag));
        } else {
            locked.add(flag);
            player.sendMessage(plugin.getLangManager().component("admin-flag-locked", "flag", flag));
        }
        plugin.getConfig().set("claims.locked-flags", locked);
        plugin.saveConfig();
        Bukkit.getScheduler().runTask(plugin, () -> AdminGui.openLockedFlags(player));
    }

    private void reopenMainMenu(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isPresent()) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openMainMenu(player, opt.get()));
        }
    }
}
