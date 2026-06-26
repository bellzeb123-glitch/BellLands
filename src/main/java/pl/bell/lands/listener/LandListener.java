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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Animals;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import pl.bell.lands.BellLands;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.integration.Pl3xMapHook;
import pl.bell.lands.model.Land;
import pl.bell.lands.model.ClaimAction;
import pl.bell.lands.manager.LandManager;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LandListener implements Listener {

    /**
     * Lets the Pro addon decide, per action, whether a TRUSTED player is allowed
     * (named claims grant granular per-trusted permissions). When no resolver is set,
     * trusted players have full access (classic behaviour).
     */
    public interface TrustedAccessResolver {
        boolean allows(Player player, Land land, ClaimAction action);
    }

    private static TrustedAccessResolver trustedResolver = null;

    public static void setTrustedResolver(TrustedAccessResolver resolver) {
        trustedResolver = resolver;
    }

    /** Central permission check used by every protection handler. */
    private boolean isAllowed(Player player, Chunk chunk, ClaimAction action) {
        LandManager lm = BellLands.getInstance().getLandManager();
        if (lm.hasBypass(player)) return true;

        Optional<Land> opt = lm.getLandAt(chunk);
        if (opt.isEmpty()) return true; // unclaimed land — no restriction

        Land land = opt.get();
        if (land.getOwner().equals(player.getUniqueId())) return true;

        if (land.isTrusted(player.getUniqueId())) {
            return trustedResolver == null || trustedResolver.allows(player, land, action);
        }

        // Stranger (guest): only what the owner explicitly opened up.
        return switch (action) {
            case BUILD -> false;
            case DOORS -> land.getFlag("guest-doors");
            case CONTAINERS -> land.getFlag("guest-chest");
            case USE -> land.getFlag("guest-use");
            case ANIMALS -> land.getFlag("guest-animals");
            case FRAMES -> land.getFlag("guest-frames");
        };
    }

    private static final Set<Material> DOOR_BLOCKS;
    private static final Set<Material> CONTAINER_BLOCKS;
    private static final Set<Material> INTERACTIVE_BLOCKS;
    static {
        var doors = EnumSet.noneOf(Material.class);
        var containers = EnumSet.noneOf(Material.class);
        var interactive = EnumSet.noneOf(Material.class);
        for (Material m : Material.values()) {
            String name = m.name();
            if (name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR"))
                doors.add(m);
            if (name.contains("CHEST") || name.contains("SHULKER_BOX"))
                containers.add(m);
            if (name.contains("CHEST") || name.contains("SHULKER_BOX") ||
                    name.contains("DOOR") || name.contains("GATE") || name.contains("TRAPDOOR") ||
                    name.contains("BUTTON") || name.contains("PLATE") || name.contains("LEVER") ||
                    name.contains("HOPPER") || name.contains("DISPENSER") || name.contains("DROPPER") ||
                    name.contains("FURNACE") || name.contains("ANVIL") || name.contains("BEACON") ||
                    name.contains("SIGN") || name.contains("BED") || name.contains("CAMPFIRE") ||
                    name.contains("COMPOSTER") || name.contains("GRINDSTONE") || name.contains("LECTERN") ||
                    name.contains("LOOM") || name.contains("STONECUTTER") || name.contains("CARTOGRAPHY") ||
                    name.contains("SMITHING"))
                interactive.add(m);
        }
        containers.add(Material.BARREL);
        interactive.add(Material.BARREL);
        interactive.add(Material.SMOKER);
        interactive.add(Material.BREWING_STAND);
        interactive.add(Material.ENCHANTING_TABLE);
        interactive.add(Material.CRAFTING_TABLE);
        interactive.add(Material.JUKEBOX);
        interactive.add(Material.NOTE_BLOCK);
        DOOR_BLOCKS = Collections.unmodifiableSet(doors);
        CONTAINER_BLOCKS = Collections.unmodifiableSet(containers);
        INTERACTIVE_BLOCKS = Collections.unmodifiableSet(interactive);
    }

    /** Solid blocks created when water and lava meet. */
    private static final Set<Material> FLUID_FORM_BLOCKS = Collections.unmodifiableSet(
        EnumSet.of(Material.OBSIDIAN, Material.COBBLESTONE, Material.STONE, Material.BASALT));

    private static final Set<UUID> particlesDisabled = ConcurrentHashMap.newKeySet();

    public static boolean toggleParticles(UUID player) {
        if (particlesDisabled.contains(player)) {
            particlesDisabled.remove(player);
            return true; // now enabled
        }
        particlesDisabled.add(player);
        return false; // now disabled
    }

    public void startParticleBorderTask() {
        Bukkit.getScheduler().runTaskTimer(BellLands.getInstance(), () -> {
            LandManager landManager = BellLands.getInstance().getLandManager();

            if (!BellLands.getInstance().getConfig().getBoolean("claims.particle-borders", true)) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (particlesDisabled.contains(player.getUniqueId())) continue;
                Chunk chunk = player.getLocation().getChunk();
                Optional<Land> opt = landManager.getLandAt(chunk);
                if (opt.isEmpty()) continue;

                Land land = opt.get();
                int bx = land.getChunkX() * 16;
                int bz = land.getChunkZ() * 16;
                World world = player.getWorld();
                double py = player.getLocation().getY();

                for (int i = 0; i <= 16; i += 2) {
                    // North
                    if (!isSameOwnerAt(landManager, land, 0, -1)) {
                        spawnBorderParticle(world, bx + i, py, bz, player);
                    }
                    // South
                    if (!isSameOwnerAt(landManager, land, 0, 1)) {
                        spawnBorderParticle(world, bx + i, py, bz + 16, player);
                    }
                    // West
                    if (!isSameOwnerAt(landManager, land, -1, 0)) {
                        spawnBorderParticle(world, bx, py, bz + i, player);
                    }
                    // East
                    if (!isSameOwnerAt(landManager, land, 1, 0)) {
                        spawnBorderParticle(world, bx + 16, py, bz + i, player);
                    }
                }
            }
        }, 20L, 20L);
    }

    private boolean isSameOwnerAt(LandManager lm, Land land, int dx, int dz) {
        Optional<Land> neighbor = lm.getLandAt(land.getWorldName(),
            land.getChunkX() + dx, land.getChunkZ() + dz);
        return neighbor.isPresent() && neighbor.get().getOwner().equals(land.getOwner());
    }

    private static final Particle.DustOptions BORDER_DUST =
        new Particle.DustOptions(org.bukkit.Color.fromRGB(155, 89, 182), 0.8f);
    private static final Particle.DustOptions OUTLINE_DUST =
        new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 170, 0), 1.2f);

    private void spawnBorderParticle(World world, double x, double y, double z, Player player) {
        Location loc = new Location(world, x, y, z);
        for (double dy = -1; dy <= 2; dy += 1.5) {
            loc.setY(y + dy);
            player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, BORDER_DUST);
        }
    }

    public void startOutlineParticleTask() {
        Bukkit.getScheduler().runTaskTimer(BellLands.getInstance(), () -> {
            LandManager landManager = BellLands.getInstance().getLandManager();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!landManager.isOutlining(player.getUniqueId())) continue;

                int[] c1 = landManager.getOutlineCorner1(player.getUniqueId());
                int[] c2 = landManager.getOutlineCorner2(player.getUniqueId());
                if (c1 == null || c2 == null) continue;

                World world = player.getWorld();
                double py = player.getLocation().getY();

                int minBX = c1[0] * 16;
                int minBZ = c1[1] * 16;
                int maxBX = (c2[0] + 1) * 16;
                int maxBZ = (c2[1] + 1) * 16;

                // Draw 4 edges of the rectangle
                for (int x = minBX; x <= maxBX; x += 2) {
                    spawnOutlineParticle(world, x, py, minBZ, player, OUTLINE_DUST);
                    spawnOutlineParticle(world, x, py, maxBZ, player, OUTLINE_DUST);
                }
                for (int z = minBZ; z <= maxBZ; z += 2) {
                    spawnOutlineParticle(world, minBX, py, z, player, OUTLINE_DUST);
                    spawnOutlineParticle(world, maxBX, py, z, player, OUTLINE_DUST);
                }
            }
        }, 10L, 10L);
    }

    private void spawnOutlineParticle(World world, double x, double y, double z, Player player, Particle.DustOptions dust) {
        Location loc = new Location(world, x, y, z);
        for (double dy = 0; dy <= 2; dy += 1.0) {
            loc.setY(y + dy);
            player.spawnParticle(Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
        }
    }

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;

        Player player = event.getPlayer();
        LandManager landManager = BellLands.getInstance().getLandManager();
        LangManager lang = BellLands.getInstance().getLangManager();
        Chunk chunk = event.getTo().getChunk();

        if (landManager.isAutoClaiming(player.getUniqueId())) {
            if (!landManager.isClaimed(chunk)) {
                int current = landManager.getClaimCount(player.getUniqueId());
                int max = landManager.getMaxClaims(player);
                if (current >= max) {
                    player.sendMessage(lang.component("claim-limit-reached", "current", current, "max", max));
                    landManager.removeAutoModes(player.getUniqueId());
                    player.sendMessage(lang.component("auto-claim-off"));
                    return;
                }
                Land land = new Land(player.getUniqueId(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                landManager.claimLand(land);
                Pl3xMapHook.drawLand(land);
                player.sendMessage(lang.component("claim-success", "x", chunk.getX(), "z", chunk.getZ()));
            }
        }

        if (landManager.isOutlining(player.getUniqueId())) {
            landManager.updateOutline(player.getUniqueId(), chunk.getX(), chunk.getZ());
            int count = landManager.getOutlineChunkCount(player.getUniqueId());
            int[] c1 = landManager.getOutlineCorner1(player.getUniqueId());
            int[] c2 = landManager.getOutlineCorner2(player.getUniqueId());
            player.sendActionBar(lang.componentRaw("outline-progress",
                "count", count,
                "x1", c1[0], "z1", c1[1],
                "x2", c2[0], "z2", c2[1]));
        }

        if (landManager.isAutoUnclaiming(player.getUniqueId())) {
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isPresent() && opt.get().getOwner().equals(player.getUniqueId())) {
                Land land = opt.get();
                landManager.unclaimLand(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
                Pl3xMapHook.removeLand(land);
                player.sendMessage(lang.component("unclaim-success"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        BellLands.getInstance().getLandManager().removeAutoModes(event.getPlayer().getUniqueId());
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
        Chunk chunk = event.getBlock().getChunk();
        if (shouldCancelAction(player, chunk)) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
            return;
        }
        if (isFluidPlacementBlocked(event.getBlock().getType(), chunk)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Material bucket = event.getBucket();
        if (bucket != Material.WATER_BUCKET && bucket != Material.LAVA_BUCKET) return;

        Material fluid = bucket == Material.WATER_BUCKET ? Material.WATER : Material.LAVA;
        Block target = event.getBlock();
        Block clicked = event.getBlockClicked();

        if (isBucketFluidDenied(player, fluid, target, clicked)) {
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

        Material mat = block.getType();
        ClaimAction action;
        if (isDoorBlock(mat)) {
            action = ClaimAction.DOORS;
        } else if (isChestBlock(mat)) {
            action = ClaimAction.CONTAINERS;
        } else if (isProtectedInteractiveBlock(mat)) {
            action = ClaimAction.USE;
        } else {
            return; // not a protected interaction
        }

        if (!isAllowed(player, block.getChunk(), action)) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
        }
    }

    // ── Entity protection: item frames, armor stands, animals, vehicles ──

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        ClaimAction action = entityAction(entity);
        if (action == null) return;
        if (!isAllowed(player, entity.getLocation().getChunk(), action)) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (!isAllowed(player, event.getRightClicked().getLocation().getChunk(), ClaimAction.FRAMES)) {
            player.sendMessage(BellLands.getInstance().getLangManager()
                .component("protection-land-belongs"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Chunk chunk = event.getEntity().getLocation().getChunk();

        if (event instanceof HangingBreakByEntityEvent byEntity
                && byEntity.getRemover() instanceof Player player) {
            if (!isAllowed(player, chunk, ClaimAction.FRAMES)) {
                event.setCancelled(true);
            }
            return;
        }

        if (isExplosionBreak(event) && isExplosionBlocked(chunk)) {
            event.setCancelled(true);
            return;
        }

        if ((event.getCause() == HangingBreakEvent.RemoveCause.OBSTRUCTION
                || event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS)
                && isLiquidInteractionBlocked(chunk)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();

        // Entity protection (item frames, armor stands, animals, vehicles)
        if (victim instanceof ItemFrame || victim instanceof Hanging || victim instanceof ArmorStand
                || isAnimalOrVehicle(victim)) {
            ClaimAction action = (victim instanceof ItemFrame || victim instanceof Hanging
                    || victim instanceof ArmorStand) ? ClaimAction.FRAMES : ClaimAction.ANIMALS;
            Player attacker = null;
            if (event.getDamager() instanceof Player p) attacker = p;
            else if (event.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;
            if (attacker == null) return;
            if (!isAllowed(attacker, victim.getLocation().getChunk(), action)) {
                event.setCancelled(true);
            }
            return;
        }

        // Player victim — PvP and mob damage
        if (!(victim instanceof Player player)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            Chunk chunk = player.getLocation().getChunk();
            LandManager landManager = BellLands.getInstance().getLandManager();
            Optional<Land> opt = landManager.getLandAt(chunk);
            if (opt.isPresent() && !opt.get().getFlag("pvp")) {
                attacker.sendMessage(BellLands.getInstance().getLangManager()
                    .component("protection-pvp-disabled"));
                event.setCancelled(true);
            }
            return;
        }

        boolean isMobAttack = event.getDamager() instanceof Monster
            || (event.getDamager() instanceof Projectile projectile
                && projectile.getShooter() instanceof Monster);
        if (!isMobAttack) return;

        Chunk chunk = player.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isPresent() && !opt.get().getFlag("mob-damage")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (!isAllowed(event.getPlayer(), event.getEntity().getLocation().getChunk(), ClaimAction.ANIMALS)) {
            event.setCancelled(true);
        }
    }

    /** Blocks fishing-rod hooking / reeling of protected entities (armor stands, frames, animals). */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        Entity target = resolveFishingTarget(event);
        if (target == null) return;

        ClaimAction action = entityAction(target);
        if (action == null) return;

        Player player = event.getPlayer();
        if (isAllowed(player, target.getLocation().getChunk(), action)) return;

        FishHook hook = event.getHook();
        if (hook != null) {
            hook.setHookedEntity(null);
        }
        event.setCancelled(true);
        player.sendMessage(BellLands.getInstance().getLangManager()
            .component("protection-land-belongs"));
    }

    /** Hooked entity from a fish event, excluding caught fish items. */
    private static Entity resolveFishingTarget(PlayerFishEvent event) {
        Entity caught = event.getCaught();
        if (caught != null) {
            if (caught instanceof Item) return null;
            return caught;
        }
        FishHook hook = event.getHook();
        return hook != null ? hook.getHookedEntity() : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) return;
        if (!isAllowed(player, event.getVehicle().getLocation().getChunk(), ClaimAction.ANIMALS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityMount(org.bukkit.event.entity.EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isAllowed(player, event.getMount().getLocation().getChunk(), ClaimAction.ANIMALS)) {
            event.setCancelled(true);
        }
    }

    /** Maps a right-clicked entity to the protection action it needs, or null if unprotected. */
    private ClaimAction entityAction(Entity entity) {
        if (entity instanceof ItemFrame || entity instanceof Hanging || entity instanceof ArmorStand) {
            return ClaimAction.FRAMES;
        }
        if (entity instanceof Villager) {
            return ClaimAction.USE; // trading
        }
        if (isAnimalOrVehicle(entity)) {
            return ClaimAction.ANIMALS;
        }
        return null;
    }

    private boolean isAnimalOrVehicle(Entity entity) {
        return entity instanceof Animals
            || entity instanceof AbstractHorse
            || entity instanceof Tameable
            || entity instanceof Vehicle;
    }

    private static boolean isDoorBlock(Material material) {
        return DOOR_BLOCKS.contains(material);
    }

    private static boolean isChestBlock(Material material) {
        return CONTAINER_BLOCKS.contains(material);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLavaDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.LAVA
            && event.getCause() != EntityDamageEvent.DamageCause.FIRE
            && event.getCause() != EntityDamageEvent.DamageCause.FIRE_TICK) {
            return;
        }

        Chunk chunk = event.getEntity().getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);

        if (opt.isPresent() && !opt.get().getFlag("lava-damage")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
            && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }

        Entity entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();
        LandManager landManager = BellLands.getInstance().getLandManager();
        Optional<Land> opt = landManager.getLandAt(chunk);
        if (opt.isEmpty()) return;

        Land land = opt.get();
        if (entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof Hanging) {
            if (!land.getFlag("explosions")) {
                event.setCancelled(true);
            }
            return;
        }

        if (!land.getFlag("explosion-damage")) {
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
    public void onBlockForm(BlockFormEvent event) {
        if (!FLUID_FORM_BLOCKS.contains(event.getNewState().getType())) return;
        if (isLiquidInteractionBlocked(event.getBlock().getChunk())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        Block fromBlock = event.getBlock();
        Block toBlock = event.getToBlock();
        Chunk fromChunk = fromBlock.getChunk();
        Chunk toChunk = toBlock.getChunk();
        boolean sameChunk = fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ();

        LandManager landManager = BellLands.getInstance().getLandManager();
        Material fromType = fromBlock.getType();

        boolean isLava = fromType == Material.LAVA;
        boolean isWater = fromType == Material.WATER;

        Optional<Land> fromOpt = landManager.getLandAt(fromChunk);
        Optional<Land> toOpt = sameChunk ? fromOpt : landManager.getLandAt(toChunk);

        if (fromOpt.isEmpty() && toOpt.isEmpty()) return;

        // Block outward flow from a protected claim (source check)
        if (isLava || isWater) {
            if (fromOpt.isPresent()) {
                Land fromLand = fromOpt.get();
                if (isLava && !fromLand.getFlag("lava-flow")) { event.setCancelled(true); return; }
                if (isWater && !fromLand.getFlag("water-flow")) { event.setCancelled(true); return; }
            }
        }

        if (toOpt.isEmpty()) return;

        Land toLand = toOpt.get();

        if (isLava && !toLand.getFlag("lava-flow")) {
            event.setCancelled(true);
            return;
        }

        if (isWater && !toLand.getFlag("water-flow")) {
            event.setCancelled(true);
            return;
        }

        // Block fluid from foreign claims into this claim
        if (!isLava && !isWater && !sameChunk) {
            if (fromOpt.isEmpty() || !fromOpt.get().getOwner().equals(toLand.getOwner())) {
                event.setCancelled(true);
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

    private boolean isExplosionBlocked(Chunk chunk) {
        Optional<Land> opt = BellLands.getInstance().getLandManager().getLandAt(chunk);
        return opt.isPresent() && !opt.get().getFlag("explosions");
    }

    private boolean isLiquidInteractionBlocked(Chunk chunk) {
        Optional<Land> opt = BellLands.getInstance().getLandManager().getLandAt(chunk);
        return opt.isPresent()
                && (!opt.get().getFlag("water-flow") || !opt.get().getFlag("lava-flow"));
    }

    private boolean isBucketFluidDenied(Player player, Material fluid, Block target, Block clicked) {
        if (isFluidDeniedAt(player, fluid, target.getChunk())) return true;
        if (!target.getChunk().equals(clicked.getChunk())
                && isFluidDeniedAt(player, fluid, clicked.getChunk())) {
            return true;
        }
        return false;
    }

    /** Build permission + water/lava-flow flags for bucket placement. */
    private boolean isFluidDeniedAt(Player player, Material fluid, Chunk chunk) {
        if (BellLands.getInstance().getLandManager().hasBypass(player)) return false;
        if (isFluidPlacementBlocked(fluid, chunk)) return true;
        return shouldCancelAction(player, chunk);
    }

    private boolean isFluidPlacementBlocked(Material material, Chunk chunk) {
        Optional<Land> opt = BellLands.getInstance().getLandManager().getLandAt(chunk);
        if (opt.isEmpty()) return false;
        Land land = opt.get();
        if (material == Material.WATER) {
            return !land.getFlag("water-flow");
        }
        if (material == Material.LAVA || material == Material.MAGMA_BLOCK) {
            return !land.getFlag("lava-flow");
        }
        return false;
    }

    private static boolean isExplosionBreak(HangingBreakEvent event) {
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION) {
            return true;
        }
        if (event instanceof HangingBreakByEntityEvent byEntity) {
            Entity remover = byEntity.getRemover();
            return remover instanceof Creeper || remover instanceof TNTPrimed || remover instanceof Explosive;
        }
        return false;
    }

    private boolean shouldCancelAction(Player player, Chunk chunk) {
        return !isAllowed(player, chunk, ClaimAction.BUILD);
    }

    private static boolean isProtectedInteractiveBlock(Material material) {
        return INTERACTIVE_BLOCKS.contains(material);
    }
}
