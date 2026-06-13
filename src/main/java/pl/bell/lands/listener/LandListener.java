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
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;
import pl.bell.lands.manager.LandManager;

import java.util.Iterator;
import java.util.Optional;

public class LandListener implements Listener {

    // ========================================================
    //  OCHRONA BUDOWANIA / NISZCZENIA
    // ========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (shouldCancelAction(player, event.getBlock().getChunk())) {
            player.sendMessage("§cTen teren nalezy do kogos innego!");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (shouldCancelAction(player, event.getBlock().getChunk())) {
            player.sendMessage("§cTen teren nalezy do kogos innego!");
            event.setCancelled(true);
        }
    }

    // ========================================================
    //  OCHRONA INTERAKCJI (flaga: use)
    // ========================================================

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

        // Wlasciciel i zaufani zawsze moga
        if (land.getOwner().equals(player.getUniqueId()) || land.isTrusted(player.getUniqueId())) return;
        if (player.isOp()) return;

        // Flaga "use" kontroluje interakcje z blokami
        if (!land.getFlag("use") && isProtectedInteractiveBlock(block.getType())) {
            player.sendMessage("§cTen teren nalezy do kogos innego!");
            event.setCancelled(true);
        }
    }

    // ========================================================
    //  PVP (flaga: pvp)
    // ========================================================

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
            attacker.sendMessage("§cPVP jest wylaczone na tym terenie!");
            event.setCancelled(true);
        }
    }

    // ========================================================
    //  OBRAZENIA OD MOBOW (flaga: mob-damage)
    // ========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Sprawdz czy atakujacy to mob (nie gracz)
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

    // ========================================================
    //  WYBUCHY (flaga: explosions)
    // ========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    // ========================================================
    //  SPAWNOWANIE MOBOW (flaga: mob-spawning)
    // ========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Nie blokuj spawnerow, jajek i pluginow — tylko naturalne
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

    // ========================================================
    //  ROZPRZESTRZENIANIE OGNIA (flaga: fire-spread)
    // ========================================================

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

    // ========================================================
    //  ROZLEWANIE CIECZY (flagi: lava-flow, water-flow)
    // ========================================================

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

        // Sprawdz czy to lava czy woda
        boolean isLava = fromType == Material.LAVA;
        boolean isWater = fromType == Material.WATER;

        if (isLava && !toLand.getFlag("lava-flow")) {
            // Lava z zewnatrz lub wewnatrz — zablokowana jesli flaga wylaczona
            Chunk fromChunk = fromBlock.getChunk();
            Optional<Land> fromOpt = landManager.getLandAt(fromChunk);
            // Blokuj jesli plynie z terenu obcego lub flaga wylaczona
            if (fromOpt.isEmpty() || !fromOpt.get().getOwner().equals(toLand.getOwner())) {
                event.setCancelled(true);
                return;
            }
        }

        if (isWater && !toLand.getFlag("water-flow")) {
            Chunk fromChunk = fromBlock.getChunk();
            Optional<Land> fromOpt = landManager.getLandAt(fromChunk);
            if (fromOpt.isEmpty() || !fromOpt.get().getOwner().equals(toLand.getOwner())) {
                event.setCancelled(true);
                return;
            }
        }

        // Ogolna ochrona — ciecz z obcego claima do naszego
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

    // ========================================================
    //  TLOKI (flaga: piston)
    // ========================================================

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

            // Sprawdz czy tlok przesuwa bloki miedzy roznymi claimami
            Optional<Land> movedLand = landManager.getLandAt(movedChunk);
            Optional<Land> pistonLand = landManager.getLandAt(pistonChunk);

            if (movedLand.isPresent()) {
                Land land = movedLand.get();
                // Jesli flaga piston jest wylaczona, blokuj tloki z zewnatrz
                if (!land.getFlag("piston")) {
                    if (pistonLand.isEmpty() || !pistonLand.get().getOwner().equals(land.getOwner())) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            // Sprawdz takze docelowy chunk (przesuniety blok)
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

    // ========================================================
    //  ROZPAD LISCI (flaga: leaf-decay)
    // ========================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("leaf-decay")) {
            event.setCancelled(true);
        }
    }

    // ========================================================
    //  METODY POMOCNICZE
    // ========================================================

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

        // Anuluj akcje, jesli gracz nie jest wlascicielem I nie jest zaufanym
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
