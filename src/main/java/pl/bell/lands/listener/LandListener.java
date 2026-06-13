package pl.bell.lands.listener;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;

import java.util.Iterator;

public class LandListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (shouldCancelAction(player, event.getBlock().getChunk())) {
            player.sendMessage("§cTen teren nalezy do kogos innego!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (shouldCancelAction(player, event.getBlock().getChunk())) {
            player.sendMessage("§cTen teren nalezy do kogos innego!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (shouldCancelAction(player, block.getChunk())) {
            Material type = block.getType();
            if (isProtectedInteractiveBlock(type)) {
                player.sendMessage("§cTen teren nalezy do kogos innego!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker == null) return;

        Chunk chunk = defender.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();

        if (landManager.isClaimed(chunk)) {
            Land land = landManager.getLandAt(chunk).orElse(null);
            if (land != null && !land.getFlag("pvp")) {
                attacker.sendMessage("§cPVP jest wylaczone na tym terenie!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler
    public void onBlockFromTo(BlockFromToEvent event) {
        Block toBlock = event.getToBlock();
        Block fromBlock = event.getBlock();
        Chunk toChunk = toBlock.getChunk();
        Chunk fromChunk = fromBlock.getChunk();

        // Blokowanie rozlewania cieczy z terenów niechronionych / cudzych do claimu
        if (toChunk.getX() != fromChunk.getX() || toChunk.getZ() != fromChunk.getZ()) {
            LandManager landManager = BellLands.getInstance().getLandManager();
            if (landManager.isClaimed(toChunk)) {
                Land toLand = landManager.getLandAt(toChunk).orElse(null);
                if (toLand != null) {
                    Land fromLand = landManager.getLandAt(fromChunk).orElse(null);
                    if (fromLand == null || !fromLand.getOwner().equals(toLand.getOwner())) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    private void handleExplosion(java.util.List<Block> blocks) {
        LandManager landManager = BellLands.getInstance().getLandManager();
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Chunk chunk = block.getChunk();
            if (landManager.isClaimed(chunk)) {
                Land land = landManager.getLandAt(chunk).orElse(null);
                if (land != null && !land.getFlag("explosions")) {
                    iterator.remove();
                }
            }
        }
    }

    private boolean shouldCancelAction(Player player, Chunk chunk) {
        if (player.isOp()) return false;

        LandManager landManager = BellLands.getInstance().getLandManager();
        if (!landManager.isClaimed(chunk)) return false;

        Land land = landManager.getLandAt(chunk).orElse(null);
        if (land == null) return false;

        return !land.getOwner().equals(player.getUniqueId());
    }

    private boolean isProtectedInteractiveBlock(Material material) {
        String name = material.name();
        return name.contains("CHEST") ||
               name.contains("SHULKER_BOX") ||
               name.contains("DOOR") ||
               name.contains("GATE") ||
               name.contains("TRAPDOOR") ||
               name.contains("BUTTON") ||
               name.contains("PLATE") ||
               name.contains("LEVER") ||
               name.contains("HOPPER") ||
               name.contains("DISPENSER") ||
               name.contains("DROPPER") ||
               name.contains("FURNACE") ||
               name.contains("ANVIL") ||
               name.contains("BEACON") ||
               material == Material.BARREL ||
               material == Material.BLAST_FURNACE ||
               material == Material.SMOKER ||
               material == Material.BREWING_STAND;
    }
}
