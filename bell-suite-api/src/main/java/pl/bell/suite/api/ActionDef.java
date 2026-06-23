package pl.bell.suite.api;

import java.util.List;

/**
 * Definicja akcji administracyjnej modulu w panelu BellHub.
 */
public record ActionDef(String id, String label, String group, boolean destructive, List<ActionField> fields) {

    public static ActionDef of(String id, String label, String group, ActionField... fields) {
        return new ActionDef(id, label, group, false, List.of(fields));
    }

    public static ActionDef destructive(String id, String label, String group, ActionField... fields) {
        return new ActionDef(id, label, group, true, List.of(fields));
    }
}
