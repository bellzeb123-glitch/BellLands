package pl.bell.lands;

import org.bukkit.plugin.java.JavaPlugin;
import pl.bell.lands.command.BellLandsCommand;
import pl.bell.lands.command.ClaimCommand;
import pl.bell.lands.command.ClaimTabCompleter;
import pl.bell.lands.command.TpaCommand;
import pl.bell.lands.config.LangManager;
import pl.bell.lands.gui.ClaimGuiListener;
import pl.bell.lands.integration.Pl3xMapHook;
import pl.bell.lands.listener.LandListener;
import pl.bell.lands.manager.LandManager;
import pl.bell.lands.manager.TPAManager;
import pl.bell.lands.manager.WarpManager;

public final class BellLands extends JavaPlugin {

    private static BellLands instance;
    private TPAManager tpaManager;
    private LandManager landManager;
    private LangManager langManager;
    private WarpManager warpManager;

    @Override
    public void onEnable() {
        instance = this;
        printBanner();

        saveDefaultConfig();

        this.langManager = new LangManager(this);
        this.tpaManager = new TPAManager(this);
        this.landManager = new LandManager();
        this.landManager.init();
        this.warpManager = new WarpManager();
        this.warpManager.init();

        TpaCommand tpaCommand = new TpaCommand(tpaManager);
        ClaimCommand claimCommand = new ClaimCommand();
        ClaimTabCompleter claimTabCompleter = new ClaimTabCompleter();
        BellLandsCommand bellLandsCommand = new BellLandsCommand();

        if (getCommand("tpa") != null) getCommand("tpa").setExecutor(tpaCommand);
        if (getCommand("tpaccept") != null) getCommand("tpaccept").setExecutor(tpaCommand);
        if (getCommand("tpdeny") != null) getCommand("tpdeny").setExecutor(tpaCommand);
        if (getCommand("claim") != null) {
            getCommand("claim").setExecutor(claimCommand);
            getCommand("claim").setTabCompleter(claimTabCompleter);
        }
        if (getCommand("belllands") != null) {
            getCommand("belllands").setExecutor(bellLandsCommand);
            getCommand("belllands").setTabCompleter(bellLandsCommand);
        }

        LandListener landListener = new LandListener();
        getServer().getPluginManager().registerEvents(landListener, this);
        landListener.startActionBarTask();
        landListener.startParticleBorderTask();
        landListener.startOutlineParticleTask();
        getServer().getPluginManager().registerEvents(new ClaimGuiListener(), this);

        Pl3xMapHook.init();

        getLogger().info("BellLands zostal pomyslnie uruchomiony!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Zapisywanie dzialek i wylaczanie BellLands...");
        if (this.landManager != null) {
            this.landManager.saveAll();
        }
    }

    public void reload() {
        reloadConfig();
        if (langManager != null) langManager.reload();
        if (tpaManager != null) tpaManager.reloadConfig();
    }

    public static BellLands getInstance() {
        return instance;
    }

    public TPAManager getTpaManager() { return tpaManager; }
    public LandManager getLandManager() { return landManager; }
    public LangManager getLangManager() { return langManager; }
    public WarpManager getWarpManager() { return warpManager; }

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
