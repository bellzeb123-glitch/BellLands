package pl.bell.lands;

import org.bukkit.plugin.java.JavaPlugin;
import pl.bell.lands.command.ClaimCommand;
import pl.bell.lands.command.TpaCommand;
import pl.bell.lands.listener.LandListener;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.TPAManager;

public final class BellLands extends JavaPlugin {

    private static BellLands instance;
    private TPAManager tpaManager;
    private LandManager landManager;

    @Override
    public void onEnable() {
        instance = this;
        printBanner();

        // INICJALIZACJA MANAGERÓW
        this.tpaManager = new TPAManager();
        this.landManager = new LandManager();

        // Rejestracja komend
        TpaCommand tpaCommand = new TpaCommand(tpaManager);
        ClaimCommand claimCommand = new ClaimCommand();
        
        if (getCommand("tpa") != null) getCommand("tpa").setExecutor(tpaCommand);
        if (getCommand("tpaccept") != null) getCommand("tpaccept").setExecutor(tpaCommand);
        if (getCommand("tpdeny") != null) getCommand("tpdeny").setExecutor(tpaCommand);
        if (getCommand("claim") != null) getCommand("claim").setExecutor(claimCommand);

        // Rejestracja listenera ochrony działek
        getServer().getPluginManager().registerEvents(new LandListener(), this);
        
        getLogger().info("BellLands zostal pomyslnie uruchomiony!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Zapisywanie dzialek i wylaczanie BellLands...");
    }

    public static BellLands getInstance() {
        return instance;
    }

    public TPAManager getTpaManager() { return tpaManager; }
    public LandManager getLandManager() { return landManager; }

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