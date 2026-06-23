package pl.bell.suite.api;

import java.util.List;
import java.util.Map;

/**
 * Kontrakt, ktory implementuje kazdy plugin Bell chcacy pojawic sie w panelu BellHub.
 *
 * <p>Rejestracja (w {@code onEnable} pluginu):
 * <pre>
 *   Bukkit.getServicesManager().register(BellModule.class, new MyModule(),
 *           this, org.bukkit.plugin.ServicePriority.Normal);
 * </pre>
 * BellHub odkrywa wszystkie zarejestrowane moduly przez ServicesManager — zero
 * twardych zaleznosci. Przyszly plugin = implementuje ten interfejs i juz jest w panelu.
 */
public interface BellModule {

    /** Stabilny identyfikator modulu, np. {@code "belllands"}. Uzywany w URL-ach API. */
    String id();

    /** Nazwa wyswietlana w nawigacji panelu, np. {@code "BellLands"}. */
    String displayName();

    /** Nazwa ikony Tabler (outline), np. {@code "map-pin"}. Bez prefiksu {@code ti-}. */
    String icon();

    /**
     * Uprawnienie wymagane, by zobaczyc/uzyc modul w panelu, np.
     * {@code "bellsuite.module.belllands"}. {@code null} = tylko {@code bellsuite.admin}.
     */
    default String permission() {
        return null;
    }

    /**
     * Statystyki pokazywane na karcie modulu na pulpicie. Wywolywane na watku serwera
     * (BellHub zapewnia bezpieczny kontekst) — nie blokuj dlugo.
     */
    List<Stat> dashboard();

    /**
     * Markery wnoszone na mape (claimy, warpy, domy, skrzynie itd.). Domyslnie brak.
     * BellHub filtruje wg {@link MapFilter} i laczy markery wszystkich modulow.
     */
    default List<MapMarker> markers(MapFilter filter) {
        return List.of();
    }

    /**
     * Widok szczegolowy modulu (JSON dla panelu), np. historia gracza lub ustawienia.
     * {@code viewId} i {@code params} sa zdefiniowane przez modul.
     */
    default String view(String viewId, Map<String, String> params) {
        return "{}";
    }

    /** Akcje administracyjne dostepne w panelu dla tego modulu. */
    default List<ActionDef> actions() {
        return List.of();
    }

    /**
     * Wykonanie akcji wywolanej z panelu (np. give, ban, delete). Domyslnie brak wsparcia.
     * Implementacja MUSI sama sprawdzic uprawnienia {@code actor} dla danej akcji.
     */
    default ActionResult invoke(SuiteAction action, Actor actor) {
        return ActionResult.error("Modul nie obsluguje akcji.");
    }
}
