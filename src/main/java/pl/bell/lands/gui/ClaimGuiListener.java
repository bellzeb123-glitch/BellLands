package pl.bell.lands.gui;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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

import java.util.*;

public class ClaimGuiListener implements Listener {

    public enum GuiType { MAIN, REMOVE_TRUSTED, ADD_TRUSTED }

    private static final Map<UUID, GuiType> openGuis = new HashMap<>();

    public static void markOpen(UUID playerId, GuiType type) {
        openGuis.put(playerId, type);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GuiType type = openGuis.get(player.getUniqueId());
        if (type == null) return;

        event.setCancelled(true);

        switch (type) {
            case MAIN -> handleMainGui(player, event);
            case REMOVE_TRUSTED -> handleRemoveTrustedGui(player, event);
            case ADD_TRUSTED -> handleAddTrustedGui(player, event);
        }
    }

    private void handleMainGui(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot > 26) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;

        Land land = opt.get();
        if (!land.getOwner().equals(player.getUniqueId()) && !player.isOp()) return;

        if (slot >= 9 && slot <= 18) {
            int flagIndex = slot - 9;
            if (flagIndex < Land.ALL_FLAGS.length) {
                String flag = Land.ALL_FLAGS[flagIndex];
                land.setFlag(flag, !land.getFlag(flag));
                landManager.saveLand(land);
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openMain(player, land));
            }
        }

        // Slot 20: add trusted
        if (slot == 20) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openAddTrusted(player, land));
        }

        // Slot 22: remove trusted
        if (slot == 22 && !land.getTrusted().isEmpty()) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openRemoveTrusted(player, land));
        }
    }

    private void handleAddTrustedGui(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.ARROW) {
            reopenMain(player);
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
                () -> ClaimGui.openMain(player, opt.get()));
        }
    }

    private void handleRemoveTrustedGui(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.ARROW) {
            reopenMain(player);
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

            land.removeTrusted(targetUuid);
            landManager.saveLand(land);

            LangManager lang = BellLands.getInstance().getLangManager();
            String name = skull.getOwningPlayer().getName();
            if (name == null) name = targetUuid.toString().substring(0, 8);
            player.sendMessage(lang.component("untrust-success", "player", name));

            if (land.getTrusted().isEmpty()) {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openMain(player, land));
            } else {
                Bukkit.getScheduler().runTask(BellLands.getInstance(),
                    () -> ClaimGui.openRemoveTrusted(player, land));
            }
        }
    }

    private void reopenMain(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isPresent()) {
            Bukkit.getScheduler().runTask(BellLands.getInstance(),
                () -> ClaimGui.openMain(player, opt.get()));
        }
    }
}
