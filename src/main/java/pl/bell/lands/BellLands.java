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
        if (this.warpManager != null) {
            this.warpManager.flushAll();
        }
        if (this.landManager != null) {
            this.landManager.saveAll();
            this.landManager.shutdown();
        }
    }

    private static final java.util.List<Runnable> reloadHooks = new java.util.ArrayList<>();

    /** Lets addons (e.g. BellLandsPro) reload their own config/lang when the core reloads. */
    public static void addReloadHook(Runnable hook) {
        reloadHooks.add(hook);
    }

    public void reload() {
        reloadConfig();
        if (langManager != null) langManager.reload();
        if (tpaManager != null) tpaManager.reloadConfig();
        for (Runnable hook : reloadHooks) {
            try { hook.run(); } catch (Exception e) {
                getLogger().warning("Reload hook failed: " + e.getMessage());
            }
        }
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
