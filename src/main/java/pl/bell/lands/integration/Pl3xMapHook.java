package pl.bell.lands.integration;

import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.world.World;
import org.bukkit.Bukkit;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Pl3xMapHook {

    private static final String LAYER_KEY = "belllands";
    private static final Map<String, SimpleLayer> layersByWorld = new HashMap<>();

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
        for (Land land : BellLands.getInstance().getLandManager().getAllLands()) {
            drawLand(land);
        }
    }

    public static void drawLand(Land land) {
        Optional<SimpleLayer> layerOpt = getLayer(land.getWorldName());
        if (layerOpt.isEmpty()) return;

        SimpleLayer layer = layerOpt.get();
        String markerKey = markerKey(land);
        int minX = land.getChunkX() * 16;
        int minZ = land.getChunkZ() * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;

        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = "Nieznany";

        var rectangle = Marker.rectangle(markerKey, Point.of(minX, minZ), Point.of(maxX, maxZ));
        rectangle.setOptions(Options.builder()
            .strokeColor(0xFFFF0000)
            .strokeWeight(2)
            .fillColor(0x33FF0000)
            .tooltipContent("Dzialka gracza: " + ownerName)
            .build());

        layer.removeMarker(markerKey);
        layer.addMarker(rectangle);
    }

    public static void removeLand(Land land) {
        getLayer(land.getWorldName()).ifPresent(layer -> layer.removeMarker(markerKey(land)));
    }

    private static String markerKey(Land land) {
        return "claim_" + land.getWorldName() + "_" + land.getChunkX() + "_" + land.getChunkZ();
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
