package de.syscall;

import de.syscall.api.VecturAPI;
import de.syscall.command.ShopCommand;
import de.syscall.listener.PlayerMoveListener;
import de.syscall.listener.ShopClickListener;
import de.syscall.listener.ShopInventoryListener;
import de.syscall.listener.ShopProtectionListener;
import de.syscall.manager.HologramManager;
import de.syscall.manager.ShopManager;
import org.bukkit.plugin.java.JavaPlugin;

public class SlownFinance extends JavaPlugin {

    private static SlownFinance instance;
    private VecturAPI vecturAPI;
    private ShopManager shopManager;
    private HologramManager hologramManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupVecturAPI()) {
            getLogger().severe("Slown-Vectur Plugin nicht gefunden!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        this.hologramManager = new HologramManager(this);
        this.shopManager = new ShopManager(this);

        registerListeners();
        registerCommands();

        getLogger().info("Slown-Finance erfolgreich gestartet!");
    }

    @Override
    public void onDisable() {
        if (hologramManager != null) {
            hologramManager.cleanup();
        }

        getLogger().info("Slown-Finance gestoppt!");
    }

    private boolean setupVecturAPI() {
        try {
            this.vecturAPI = VecturAPI.getInstance();
            return vecturAPI != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ShopClickListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopProtectionListener(this), this);
    }

    private void registerCommands() {
        ShopCommand shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getCommand("shop").setTabCompleter(shopCommand);
    }

    public void reload() {
        reloadConfig();
        shopManager.reloadConfig();
    }

    public static SlownFinance getInstance() {
        return instance;
    }

    public VecturAPI getVecturAPI() {
        return vecturAPI;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }
}