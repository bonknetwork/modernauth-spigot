package org.bonkmc.modernAuthentication;

import org.bukkit.plugin.java.JavaPlugin;

public final class ModernAuthentication extends JavaPlugin {

    private String backendUrl;
    private int backendPort;
    private String accessCode;
    // The public server ID used in the link sent to players.
    private static final String SERVER_ID = "bonk-network";
    private AuthListener authListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        authListener = new AuthListener(this);
        getServer().getPluginManager().registerEvents(authListener, this);

        getCommand("authreload").setExecutor(new ReloadCommand(this));
    }

    @Override
    public void onDisable() {
        // Optional cleanup.
    }

    public void loadConfiguration() {
        backendUrl = getConfig().getString("backendUrl", "http://127.0.0.1");
        backendPort = getConfig().getInt("backendPort", 3000);
        accessCode = getConfig().getString("access-code", "");
        getLogger().info("Configuration loaded: backendUrl=" + backendUrl +
                ", backendPort=" + backendPort +
                ", accessCode=" + accessCode);
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public int getBackendPort() {
        return backendPort;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public String getServerId() {
        return SERVER_ID;
    }

    public AuthListener getAuthListener() {
        return authListener;
    }
}
