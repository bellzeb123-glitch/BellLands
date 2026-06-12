package pl.bell.lands;

import org.bukkit.plugin.java.JavaPlugin;

public final class BellLands extends JavaPlugin {

    private static BellLands instance;

    @Override
    public void onEnable() {
        instance = this;
        printBanner();

        // INICJALIZACJA ETAPÓW
        // TODO: Ładowanie bazy danych
        // TODO: Rejestracja komend (TPA, BellLands)
        // TODO: Rejestracja listenerów (ochrona terenu)
        
        getLogger().info("BellLands zostal pomyslnie uruchomiony!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Zapisywanie dzialek i wylaczanie BellLands...");
        // TODO: Zapis bazy danych
    }

    public static BellLands getInstance() {
        return instance;
    }

    private void printBanner() {
        var c = org.bukkit.Bukkit.getConsoleSender();
        c.sendMessage("§r");
        c.sendMessage("§6  ██████╗ ███████╗██╗     ██╗          ");
        c.sendMessage("§6  ██╔══██╗██╔════╝██║     ██║          ");
        c.sendMessage("§6  ██████╔╝█████╗  ██║     ██║          ");
        c.sendMessage("§6  ██╔══██╗██╔══╝  ██║     ██║          ");
        c.sendMessage("§6  ██████╔╝███████╗███████╗███████╗§r§f Lands");
        c.sendMessage("§6  ╚═════╝ ╚══════╝╚══════╝╚══════╝     ");
        c.sendMessage("§r");
        c.sendMessage("§7  Version §f" + getDescription().getVersion() + "  §7│  Author §bBellzeb");
        c.sendMessage("§7  Status  §aFree §7│ §7Pro §5Addon");
        c.sendMessage("§r");
    }
}