package net.okocraft.chestshopsearcher;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.chestshopsearcher.command.CSSCommand;
import net.okocraft.chestshopsearcher.config.Config;
import net.okocraft.chestshopsearcher.database.ShopManager;
import net.okocraft.chestshopsearcher.listener.ChestShopListener;
import org.jetbrains.annotations.Nullable;

public class Main extends JavaPlugin {

    @Nullable
    private static Main instance;

    private Logger log;

    @Override
    public void onEnable() {
        log = getLogger();

        saveDefaultConfig();

        // init database.
        ShopManager.getInstance();
        log.info("We are using " + (Config.getInstance().isUsingMySQL() ? "MySQL" : "SQLite"));

        ChestShopListener.getInstance().start();

        CSSCommand.init();

        // GO GO GO
        log.info("ChestShopSearcher has been enabled!");
    }

    @Override
    public void onDisable() {
        ShopManager.getInstance().close();
        log.info("ChestShopSearcher has been disabled!");
    }

    public static Main getInstance() {
        if (instance == null) {
            instance = (Main) Bukkit.getPluginManager().getPlugin("ChestShopSearcher");
            if (instance == null) {
                throw new IllegalStateException("ChestShopSearcher is not enabled.");
            }
        }

        return instance;
    }
}
