package pl.bell.suite.api;

import java.util.Set;
import java.util.UUID;

/**
 * Tozsamosc wywolujacego akcje z panelu. Moduly uzywaja {@link #has(String)} do
 * sprawdzania uprawnien przed wykonaniem akcji.
 *
 * @param name        nazwa konta panelu (np. nick admina)
 * @param uuid        UUID gracza, jesli powiazany; moze byc {@code null}
 * @param admin       czy ma {@code bellsuite.admin}
 * @param permissions zbior nadanych uprawnien
 */
public record Actor(String name, UUID uuid, boolean admin, Set<String> permissions) {

    public boolean has(String permission) {
        return admin || (permissions != null && permissions.contains(permission));
    }

    public static Actor adminConsole() {
        return new Actor("console", null, true, Set.of());
    }
}
