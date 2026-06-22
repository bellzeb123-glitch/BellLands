package pl.bell.suite.api;

import java.util.Map;

/**
 * Akcja zlecona z panelu do modulu, np. {@code name="give"} z parametrami.
 *
 * @param module docelowy modul (np. "market")
 * @param name   nazwa akcji (np. "give-coins")
 * @param params parametry akcji (klucz->wartosc, surowe stringi z panelu)
 */
public record SuiteAction(String module, String name, Map<String, String> params) {

    public String param(String key) {
        return params == null ? null : params.get(key);
    }

    public String param(String key, String def) {
        String v = param(key);
        return v == null ? def : v;
    }
}
