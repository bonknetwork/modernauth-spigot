package org.bonkmc.modernAuthentication;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class ModernAuthentication extends JavaPlugin {

    private Connection connection;

    @Override
    public void onEnable() {
        // Initialize the plugin's SQLite database.
        initDatabase();

        // Register the player join listener.
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // Schedule a repeating task (every 10 seconds) to poll the Python API for login status.
        new AuthCheckTask(this).runTaskTimer(this, 20L, 200L);
    }

    @Override
    public void onDisable() {
        // Close the database connection if open.
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    private void initDatabase() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "auth.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            Statement stmt = connection.createStatement();
            // Create a table for storing player tokens and login status.
            stmt.execute("CREATE TABLE IF NOT EXISTS players ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "token TEXT, "
                    + "authenticated INTEGER DEFAULT 0"
                    + ")");
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
