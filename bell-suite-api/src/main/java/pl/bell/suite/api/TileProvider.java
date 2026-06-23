package pl.bell.suite.api;

/**
 * Zrodlo kafelkow terenu dla mapy panelu. BellHub renderuje tylko nakladke Bell
 * (markery); teren dostarcza zewnetrzny renderer. Pierwsza implementacja celuje w
 * Pl3xMap, ale interfejs zostawia miejsce na wlasny renderer kafelkow w przyszlosci.
 *
 * <p>Rejestracja przez ServicesManager, jak {@link BellModule}.
 */
public interface TileProvider {

    /** Identyfikator, np. "pl3xmap". */
    String id();

    /** Czy renderer jest dostepny w tym momencie (np. plugin obecny i wlaczony). */
    boolean available();

    /**
     * Szablon URL kafelka w formacie Leaflet, np.
     * {@code "http://host:8080/tiles/{world}/{zoom}/{x}_{y}.png"}.
     * BellHub/Leaflet podstawia {z}/{x}/{y}; {world} podstawia panel.
     */
    String tileUrlTemplate(String world);

    default int minZoom() { return 0; }

    default int maxZoom() { return 3; }
}
