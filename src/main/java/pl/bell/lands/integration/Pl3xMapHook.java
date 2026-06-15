package pl.bell.lands.integration;

import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.world.World;
import org.bukkit.Bukkit;
import pl.bell.lands.BellLands;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.model.Land;

import java.util.*;

public class Pl3xMapHook {

    private static final String LAYER_KEY = "belllands";
    private static final Map<String, SimpleLayer> layersByWorld = new HashMap<>();

    private static final int FILL_COLOR = 0x449B59B6;
    private static final int STROKE_COLOR = 0xCC7B39B6;
    private static final int STROKE_WEIGHT = 3;

    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("Pl3xMap") == null) {
            BellLands.getInstance().getLogger().info("Pl3xMap nie jest zainstalowany — mapa claimow wylaczona.");
            return;
        }

        Bukkit.getScheduler().runTaskLater(BellLands.getInstance(), () -> {
            try {
                for (World world : Pl3xMap.api().getWorldRegistry()) {
                    registerWorld(world);
                }
                drawAll();
                BellLands.getInstance().getLogger().info("Polaczono z Pl3xMap! Dzialki sa widoczne na mapie.");
            } catch (Exception e) {
                BellLands.getInstance().getLogger().warning("Nie udalo sie polaczyc z Pl3xMap: " + e.getMessage());
            }
        }, 40L);
    }

    private static void registerWorld(World world) {
        String worldName = world.getName();
        if (world.getLayerRegistry().has(LAYER_KEY)) {
            layersByWorld.put(worldName, (SimpleLayer) world.getLayerRegistry().get(LAYER_KEY));
            return;
        }

        SimpleLayer layer = new SimpleLayer(LAYER_KEY, () -> "BellLands Claims");
        world.getLayerRegistry().register(LAYER_KEY, layer);
        layersByWorld.put(worldName, layer);
    }

    public static void drawAll() {
        for (SimpleLayer layer : layersByWorld.values()) {
            layer.clearMarkers();
        }

        LandManager landManager = BellLands.getInstance().getLandManager();

        for (Land land : landManager.getAllLands()) {
            drawFill(land);
        }

        Set<String> processedEdges = new HashSet<>();
        for (Land land : landManager.getAllLands()) {
            drawOuterEdges(land, landManager, processedEdges);
        }
    }

    public static void drawLand(Land land) {
        drawAll();
    }

    public static void removeLand(Land land) {
        drawAll();
    }

    private static void drawFill(Land land) {
        Optional<SimpleLayer> layerOpt = getLayer(land.getWorldName());
        if (layerOpt.isEmpty()) return;

        SimpleLayer layer = layerOpt.get();
        int minX = land.getChunkX() * 16;
        int minZ = land.getChunkZ() * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;

        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = "Nieznany";

        String key = "fill_" + land.getWorldName() + "_" + land.getChunkX() + "_" + land.getChunkZ();
        var rectangle = Marker.rectangle(key, Point.of(minX, minZ), Point.of(maxX, maxZ));
        rectangle.setOptions(Options.builder()
            .strokeWeight(0)
            .fillColor(FILL_COLOR)
            .tooltipContent("Dzialka gracza: " + ownerName)
            .build());

        layer.addMarker(rectangle);
    }

    private static void drawOuterEdges(Land land, LandManager landManager, Set<String> processedEdges) {
        Optional<SimpleLayer> layerOpt = getLayer(land.getWorldName());
        if (layerOpt.isEmpty()) return;

        SimpleLayer layer = layerOpt.get();
        int cx = land.getChunkX();
        int cz = land.getChunkZ();
        String world = land.getWorldName();
        UUID owner = land.getOwner();

        int bx = cx * 16;
        int bz = cz * 16;

        // North edge (z = bz): neighbor at cz-1
        if (!isSameOwner(landManager, world, cx, cz - 1, owner)) {
            addEdge(layer, processedEdges, world, cx, cz, "N",
                Point.of(bx, bz), Point.of(bx + 16, bz));
        }
        // South edge (z = bz+16): neighbor at cz+1
        if (!isSameOwner(landManager, world, cx, cz + 1, owner)) {
            addEdge(layer, processedEdges, world, cx, cz, "S",
                Point.of(bx, bz + 16), Point.of(bx + 16, bz + 16));
        }
        // West edge (x = bx): neighbor at cx-1
        if (!isSameOwner(landManager, world, cx - 1, cz, owner)) {
            addEdge(layer, processedEdges, world, cx, cz, "W",
                Point.of(bx, bz), Point.of(bx, bz + 16));
        }
        // East edge (x = bx+16): neighbor at cx+1
        if (!isSameOwner(landManager, world, cx + 1, cz, owner)) {
            addEdge(layer, processedEdges, world, cx, cz, "E",
                Point.of(bx + 16, bz), Point.of(bx + 16, bz + 16));
        }
    }

    private static boolean isSameOwner(LandManager lm, String world, int cx, int cz, UUID owner) {
        Optional<Land> neighbor = lm.getLandAt(world, cx, cz);
        return neighbor.isPresent() && neighbor.get().getOwner().equals(owner);
    }

    private static void addEdge(SimpleLayer layer, Set<String> processed,
                                String world, int cx, int cz, String dir, Point a, Point b) {
        String edgeKey = "edge_" + world + "_" + cx + "_" + cz + "_" + dir;
        if (!processed.add(edgeKey)) return;

        var line = Marker.polyline(edgeKey, a, b);
        line.setOptions(Options.builder()
            .strokeColor(STROKE_COLOR)
            .strokeWeight(STROKE_WEIGHT)
            .build());
        layer.addMarker(line);
    }

    private static Optional<SimpleLayer> getLayer(String worldName) {
        SimpleLayer layer = layersByWorld.get(worldName);
        if (layer != null) return Optional.of(layer);

        World world = Pl3xMap.api().getWorldRegistry().get(worldName);
        if (world != null) {
            registerWorld(world);
            return Optional.ofNullable(layersByWorld.get(worldName));
        }
        return Optional.empty();
    }
}
