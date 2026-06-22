package pl.bell.suite.api;

import java.util.Set;

/**
 * Filtr przekazywany do {@link BellModule#markers(MapFilter)} — okresla, ktore warstwy
 * i ktory swiat panel chce pokazac. Modul powinien zwracac tylko pasujace markery.
 *
 * @param world        nazwa swiata; {@code null} = wszystkie
 * @param enabledLayers wlaczone warstwy (np. "warps","homes"); pusty zbior = wszystkie
 */
public record MapFilter(String world, Set<String> enabledLayers) {

    public boolean wants(String layer) {
        return enabledLayers == null || enabledLayers.isEmpty() || enabledLayers.contains(layer);
    }

    public boolean wantsWorld(String w) {
        return world == null || world.isEmpty() || world.equals(w);
    }
}
