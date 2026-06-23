package pl.bell.suite.api;

import java.util.List;

/**
 * Pole formularza akcji w panelu BellHub (np. nick gracza, limit, przelacznik).
 */
public record ActionField(String key, String label, Kind kind, List<String> options) {

    public enum Kind {
        TEXT, PLAYER, NUMBER, BOOL, SELECT
    }

    public static ActionField text(String key, String label) {
        return new ActionField(key, label, Kind.TEXT, List.of());
    }

    public static ActionField player(String key, String label) {
        return new ActionField(key, label, Kind.PLAYER, List.of());
    }

    public static ActionField number(String key, String label) {
        return new ActionField(key, label, Kind.NUMBER, List.of());
    }

    public static ActionField bool(String key, String label) {
        return new ActionField(key, label, Kind.BOOL, List.of());
    }

    public static ActionField select(String key, String label, List<String> options) {
        return new ActionField(key, label, Kind.SELECT, List.copyOf(options));
    }
}
