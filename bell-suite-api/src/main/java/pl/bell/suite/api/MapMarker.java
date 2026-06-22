package pl.bell.suite.api;

import java.util.Map;

/**
 * Marker wnoszony przez modul na mape panelu. Modul nie renderuje terenu — dostarcza
 * tylko punkty/obszary Bell (warpy, domy, claimy, regiony, skrzynie VIP, gracze).
 *
 * @param layer warstwa filtra, np. "warps", "homes", "claims", "regions", "vipchests", "players"
 * @param type  "point" | "polygon" | "rect"
 * @param label etykieta pokazywana w dymku
 * @param world nazwa swiata
 * @param x     wspolrzedna X (dla point/rect — srodek lub naroznik)
 * @param y     wysokosc (informacyjnie, mapa 2D moze ignorowac)
 * @param z     wspolrzedna Z
 * @param color kolor HEX markera, np. "#3FC9FF"
 * @param shape dla polygon/rect: pary [x,z] punktow; dla point puste
 * @param meta  dodatkowe pola (owner, flagi itp.) do dymka/akcji
 */
public record MapMarker(
        String layer,
        String type,
        String label,
        String world,
        double x,
        double y,
        double z,
        String color,
        double[][] shape,
        Map<String, String> meta
) {
    public static MapMarker point(String layer, String label, String world,
                                  double x, double y, double z, String color) {
        return new MapMarker(layer, "point", label, world, x, y, z, color, new double[0][], Map.of());
    }
}
