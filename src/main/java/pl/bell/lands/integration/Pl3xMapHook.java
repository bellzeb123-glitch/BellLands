package pl.bell.lands.integration;

import dev.pl3x.map.api.Pl3xMap;
import dev.pl3x.map.api.Key;
import dev.pl3x.map.api.SimpleLayer;
import dev.pl3x.map.api.MapWorld;
import dev.pl3x.map.api.marker.Marker;
import dev.pl3x.map.api.marker.Rectangle;
import dev.pl3x.map.api.point.Point;
import org.bukkit.Bukkit;
import pl.bell.lands.BellLands;
import pl.bell.lands.model.Land;

public class Pl3xMapHook {

    private static SimpleLayer layer;
    private static final Key LAYER_KEY = Key.of("belllands");

    public static void init() {
        if (Bukkit.getPluginManager().getPlugin("Pl3xMap") == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(BellLands.getInstance(), () -> {
            try {
                Pl3xMap.api().getWorldRegistry().forEach(Pl3xMapHook::registerWorld);
                drawAll();
                BellLands.getInstance().getLogger().info("Polaczono z Pl3xMap! Dzialki sa teraz widoczne na mapie.");
            } catch (Exception e) {
                BellLands.getInstance().getLogger().warning("Nie udalo sie polaczyc z Pl3xMap: " + e.getMessage());
            }
        }, 40L);
    }

    private static void registerWorld(MapWorld world) {
        if (world.layerRegistry().has(LAYER_KEY)) return;
        
        // Utworzenie warstwy z nazwa wyswietlana
        layer = new SimpleLayer("Dzialki BellLands", () -> "BellLands Claims");
        world.layerRegistry().register(LAYER_KEY, layer);
    }

    public static void drawAll() {
        if (layer == null) return;
        layer.clear(); // Wyczyszczenie warstwy
        for (Land land : BellLands.getInstance().getLandManager().getAllLands()) {
            drawLand(land);
        }
    }

    public static void drawLand(Land land) {
        if (layer == null) return;
        
        int minX = land.getChunkX() * 16;
        int minZ = land.getChunkZ() * 16;
        int maxX = minX + 16;
        int maxZ = minZ + 16;

        Key markerKey = Key.of("claim_" + land.getWorldName() + "_" + land.getChunkX() + "_" + land.getChunkZ());
        
        // Utworzenie prostokata na podstawie współrzędnych chunków
        Rectangle rectangle = Marker.rectangle(Point.of(minX, minZ), Point.of(maxX, maxZ));
        
        // Stylizacja prostokata (czerwona ramka, polprzezroczyste wypelnienie)
        rectangle.getOptions().setStrokeColor(0xFFFF0000); // ARGB - czerwony
        rectangle.getOptions().setFillColor(0x33FF0000);   // ARGB - polprzezroczysty czerwony
        rectangle.getOptions().setStrokeWeight(2);
        
        String ownerName = Bukkit.getOfflinePlayer(land.getOwner()).getName();
        if (ownerName == null) ownerName = "Nieznany";
        rectangle.getOptions().setTooltip("Dzialka gracza: " + ownerName);

        // Rejestracja markera na warstwie
        layer.register(markerKey, rectangle);
    }

    public static void removeLand(Land land) {
        if (layer == null) return;
        Key markerKey = Key.of("claim_" + land.getWorldName() + "_" + land.getChunkX() + "_" + land.getChunkZ());
        layer.unregister(markerKey);
    }
}
