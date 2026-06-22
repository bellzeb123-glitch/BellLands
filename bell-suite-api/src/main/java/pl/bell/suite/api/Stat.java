package pl.bell.suite.api;

/**
 * Pojedyncza statystyka na karcie modulu na pulpicie panelu.
 *
 * @param label  etykieta, np. "Claimy"
 * @param value  wartosc do wyswietlenia (sformatowana), np. "1 284"
 * @param accent akcent koloru: "cyan" | "violet" | "gold" | "green" | "silver" (mapowane w UI)
 */
public record Stat(String label, String value, String accent) {

    public static Stat of(String label, String value) {
        return new Stat(label, value, "cyan");
    }

    public static Stat of(String label, long value, String accent) {
        return new Stat(label, Long.toString(value), accent);
    }
}
