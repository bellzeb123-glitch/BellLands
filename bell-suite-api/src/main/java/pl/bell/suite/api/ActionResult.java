package pl.bell.suite.api;

import java.util.Map;

/**
 * Wynik akcji modulu zwracany do panelu.
 *
 * @param ok      czy akcja sie powiodla
 * @param message komunikat dla uzytkownika (lokalizowany po stronie modulu)
 * @param data    opcjonalne dodatkowe dane do UI
 */
public record ActionResult(boolean ok, String message, Map<String, String> data) {

    public static ActionResult ok(String message) {
        return new ActionResult(true, message, Map.of());
    }

    public static ActionResult ok(String message, Map<String, String> data) {
        return new ActionResult(true, message, data);
    }

    public static ActionResult error(String message) {
        return new ActionResult(false, message, Map.of());
    }
}
