package pl.bell.lands;

import org.bukkit.plugin.java.JavaPlugin;
import pl.bell.lands.command.TpaCommand;
import pl.bell.lands.manager.TPAManager;

public class BellLands extends JavaPlugin {

    private TPAManager tpaManager;

    @Override
    public void onEnable() {
        // Inicjalizacja managera
        this.tpaManager = new TPAManager();

        // Rejestracja komend
        TpaCommand tpaCommand = new TpaCommand(tpaManager);
        
        if (getCommand("tpa") != null) getCommand("tpa").setExecutor(tpaCommand);
        if (getCommand("tpaccept") != null) getCommand("tpaccept").setExecutor(tpaCommand);
        if (getCommand("tpdeny") != null) getCommand("tpdeny").setExecutor(tpaCommand);

        getLogger().info("BellLands zostalo pomyslnie uruchomione!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BellLands zostalo wylaczone.");
    }
}
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