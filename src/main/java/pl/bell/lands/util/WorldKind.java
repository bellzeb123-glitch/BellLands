package pl.bell.lands.util;

import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * Etykieta wymiaru claimu: Overworld / Nether / End (po nazwie świata lub Environment).
 */
public final class WorldKind {

    public enum Kind { OVERWORLD, NETHER, END, UNKNOWN }

    private WorldKind() {}

    public static Kind of(String worldName) {
        if (worldName == null || worldName.isBlank()) return Kind.UNKNOWN;
        World bukkit = Bukkit.getWorld(worldName);
        if (bukkit != null) {
            return switch (bukkit.getEnvironment()) {
                case NETHER -> Kind.NETHER;
                case THE_END -> Kind.END;
                case NORMAL -> Kind.OVERWORLD;
                default -> Kind.UNKNOWN;
            };
        }
        String lower = worldName.toLowerCase();
        if (lower.endsWith("_nether") || lower.contains("nether")) return Kind.NETHER;
        if (lower.endsWith("_the_end") || lower.endsWith("_end") || lower.contains("the_end")) return Kind.END;
        return Kind.OVERWORLD;
    }

    /** Krótka etykieta PL/EN-agnostyczna do GUI (kolor legacy &). */
    public static String labelColored(String worldName) {
        return switch (of(worldName)) {
            case NETHER -> "&cNether";
            case END -> "&5End";
            case OVERWORLD -> "&aOverworld";
            case UNKNOWN -> "&7?";
        };
    }

    /** Tekst bez kolorów (Hub / JSON). */
    public static String labelPlain(String worldName) {
        return switch (of(worldName)) {
            case NETHER -> "Nether";
            case END -> "End";
            case OVERWORLD -> "Overworld";
            case UNKNOWN -> "Unknown";
        };
    }

    public static String labelKey(String worldName) {
        return of(worldName).name().toLowerCase();
    }
}
