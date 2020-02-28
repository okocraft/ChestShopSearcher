package net.okocraft.chestshopsearcher.config;

import net.okocraft.configurationapi.BaseConfig;
import net.okocraft.chestshopsearcher.Main;
import org.jetbrains.annotations.NotNull;

public class Config extends BaseConfig {

    private static Main plugin = Main.getInstance();
    @NotNull
    private static Config instance = new Config();

    private Config() {
        super("config.yml", plugin.getDataFolder(), plugin.getResource("config.yml"));
    }

    @NotNull
    public static Config getInstance() {
        return instance;
    }

    public boolean isUsingMySQL() {
        return getConfig().getBoolean("database.use-mysql", false);
    }

    public String getMySQLHost() {
        return getConfig().getString("database.mysql.host", "localhost");
    }

    public String getMySQLUser() {
        return getConfig().getString("database.mysql.user", "root");
    }

    public String getMySQLPassword() {
        return getConfig().getString("database.mysql.pass", "");
    }

    public int getMySQLPort() {
        return getConfig().getInt("database.mysql.port", 3306);
    }

    public String getDatabaseName() {
        return getConfig().getString("database.db-name", "chestshopsearcher");
    }

    public boolean isSQLLoggingEnabled() {
        return getConfig().getBoolean("database.sql-logging");
    }

    public void reloadAllConfigs() {
        Messages.getInstance().reload();
        reloadConfig();
    }

}