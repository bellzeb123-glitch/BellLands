package pl.bell.lands.listener;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Bukkit;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;

import java.util.Iterator;
import java.util.Optional;

public class LandListener implements Listener {

    public void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(BellLands.getInstance(), () -> {
            LandManager landManager = BellLands.getInstance().getLandManager();
            LangManager lang = BellLands.getInstance().getLangManager();

            for (Player player : Bukkit.getOnlinePlayers()) {
                Chunk chunk = player.getLocation().getChunk();
                Optional<Land> opt = landManager.getLandAt(chunk);

                if (opt.isPresent()) {
                    Land land = opt.get();
                    if (land.getOwner().equals(player.getUniqueId())) {
                        player.sendActionBar(lang.componentRaw("actionbar-own-land"));
                    } else {
                        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
                        if (ownerName == null) ownerName = lang.getRaw("info-unknown-owner");
                        player.sendActionBar(lang.componentRaw("actionbar-other-land", "owner", ownerName));
                    }
                }
            }
        }, 30L, 30L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (shouldCancelAction(player, event.getBlock().getChunk())) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (shouldCancelAction(player, event.getBlock().getChunk())) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        Chunk chunk = block.getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;

        Land land = opt.get();
        if (land.getOwner().equals(player.getUniqueId()) || land.isTrusted(player.getUniqueId())) return;
        if (player.isOp()) return;

        if (!land.getFlag("use") && isProtectedInteractiveBlock(block.getType())) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
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
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("pvp")) {
            attacker.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-pvp-disabled"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        boolean isMobAttack = false;
        if (event.getDamager() instanceof Monster) {
            isMobAttack = true;
        } else if (event.getDamager() instanceof Projectile projectile
                   && projectile.getShooter() instanceof Monster) {
            isMobAttack = true;
        }

        if (!isMobAttack) return;

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("mob-damage")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
            && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Chunk chunk = event.getEntity().getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("explosion-damage")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL
            && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.REINFORCEMENTS) {
            return;
        }

        if (!(event.getEntity() instanceof Monster)) return;

        Chunk chunk = event.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("mob-spawning")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.SPREAD
            && event.getCause() != BlockIgniteEvent.IgniteCause.LAVA) {
            return;
        }

        Chunk chunk = event.getBlock().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("fire-spread")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();
        Block toBlock = event.getToBlock();
        Chunk toChunk = toBlock.getChunk();

        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(toChunk);
        if (opt.isEmpty()) return;

        Land toLand = opt.get();
        Material fromType = fromBlock.getType();

        boolean isLava = fromType == Material.LAVA;
        boolean isWater = fromType == Material.WATER;

        if (isLava && !toLand.getFlag("lava-flow")) {
            event.setCancelled(true);
            return;
        }

        if (isWater && !toLand.getFlag("water-flow")) {
            event.setCancelled(true);
            return;
        }

        // Block fluid from foreign claims into this claim
        if (!isLava && !isWater) {
            Chunk fromChunk = fromBlock.getChunk();
            if (fromChunk.getX() != toChunk.getX() || fromChunk.getZ() != toChunk.getZ()) {
                Optional<Land> fromOpt = landManager.getLandAt(fromChunk);
                if (fromOpt.isEmpty() || !fromOpt.get().getOwner().equals(toLand.getOwner())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        handlePiston(event.getBlock(), event.getBlocks(), event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        handlePiston(event.getBlock(), event.getBlocks(), event);
    }

    private void handlePiston(Block pistonBlock, java.util.List<Block> movedBlocks, BlockPistonEvent event) {
        LandManager landManager = BellLands.getInstance().getLandManager();
        Chunk pistonChunk = pistonBlock.getChunk();

        for (Block moved : movedBlocks) {
            Chunk movedChunk = moved.getChunk();
            Optional<Land> movedLand = landManager.getLandAt(movedChunk);
            Optional<Land> pistonLand = landManager.getLandAt(pistonChunk);

            if (movedLand.isPresent()) {
                Land land = movedLand.get();
                if (!land.getFlag("piston")) {
                    if (pistonLand.isEmpty() || !pistonLand.get().getOwner().equals(land.getOwner())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            Block destination = moved.getRelative(event.getDirection());
            Chunk destChunk = destination.getChunk();
            Optional<Land> destLand = landManager.getLandAt(destChunk);

            if (destLand.isPresent()) {
                Land land = destLand.get();
                if (!land.getFlag("piston")) {
                    if (pistonLand.isEmpty() || !pistonLand.get().getOwner().equals(land.getOwner())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("leaf-decay")) {
            event.setCancelled(true);
        }
    }

    private void handleExplosion(java.util.List<Block> blocks) {
        LandManager landManager = BellLands.getInstance().getLandManager();
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Chunk chunk = block.getChunk();
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isPresent() && !opt.get().getFlag("explosions")) {
                iterator.remove();
            }
        }
    }

    private boolean shouldCancelAction(Player player, Chunk chunk) {
        if (player.isOp()) return false;

        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return false;

        Land land = opt.get();
        return !land.getOwner().equals(player.getUniqueId()) && !land.isTrusted(player.getUniqueId());
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
               name.contains("SIGN") ||
               name.contains("BED") ||
               name.contains("CAMPFIRE") ||
               name.contains("COMPOSTER") ||
               name.contains("GRINDSTONE") ||
               name.contains("LECTERN") ||
               name.contains("LOOM") ||
               name.contains("STONECUTTER") ||
               name.contains("CARTOGRAPHY") ||
               name.contains("SMITHING") ||
               material == Material.BARREL ||
               material == Material.BLAST_FURNACE ||
               material == Material.SMOKER ||
               material == Material.BREWING_STAND ||
               material == Material.ENCHANTING_TABLE ||
               material == Material.CRAFTING_TABLE ||
               material == Material.JUKEBOX ||
               material == Material.NOTE_BLOCK;
    }
}
