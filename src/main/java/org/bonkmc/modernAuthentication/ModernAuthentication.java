package org.bonkmc.modernAuthentication;

import org.bukkit.plugin.java.JavaPlugin;

public final class ModernAuthentication extends JavaPlugin {

    private int authTimeout;
    private String backendUrl;
    private int backendPort;
    private AuthListener authListener; // Store the instance

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        // Create and register your AuthListener, then store the instance.
        authListener = new AuthListener(this);
        getServer().getPluginManager().registerEvents(authListener, this);

        // Register reload command
        getCommand("authreload").setExecutor(new ReloadCommand(this));
    }

    @Override
    public void onDisable() {
        // Optionally cancel any pending tasks.
    }

    public void loadConfiguration() {
        authTimeout = getConfig().getInt("authTimeout", 60);
        backendUrl = getConfig().getString("backendUrl", "http://127.0.0.1");
        backendPort = getConfig().getInt("backendPort", 3000);
        getLogger().info("Configuration loaded: authTimeout=" + authTimeout +
                ", backendUrl=" + backendUrl +
                ", backendPort=" + backendPort);
    }

    public int getAuthTimeout() {
        return authTimeout;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public int getBackendPort() {
        return backendPort;
    }

    // Provide a getter for AuthListener
    public AuthListener getAuthListener() {
        return authListener;
    }
}
