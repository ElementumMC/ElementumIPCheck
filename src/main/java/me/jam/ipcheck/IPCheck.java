package me.jam.ipcheck;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Paths;
import java.sql.SQLException;

public final class IPCheck extends JavaPlugin {

    public static IPCheck plugin;

    private DatabaseManager database;

    public static final String prefix = ChatColor.AQUA + "[" + ChatColor.RED + ChatColor.BOLD + "IPCheck" + ChatColor.AQUA + "] ";

    @Override
    public void onEnable() {
        plugin = this;


        if(!getDataFolder().exists()) getDataFolder().mkdir();

        try {
            database = new DatabaseManager(
                    Paths.get(getDataFolder().toPath().toString(), "db.db").toAbsolutePath().toString()
            );

            getCommand("ipc").setExecutor(new CmdManager());

            Bukkit.getPluginManager().registerEvents(new EventsManager(), this);

            getLogger().info("IPCheck has been enabled.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DatabaseManager getDatabase() {
        return database;
    }
}
