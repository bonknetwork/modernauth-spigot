package org.bonkmc.modernAuthentication;

import org.bukkit.plugin.java.JavaPlugin;

public final class ModernAuthentication extends JavaPlugin {

    private String backendUrl;
    private int backendPort;
    private String accessCode;
    private String serverId; // Now loaded from config
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
        serverId = getConfig().getString("server-id", "bonk-network");
        getLogger().info("Configuration loaded: backendUrl=" + backendUrl +
                ", backendPort=" + backendPort +
                ", accessCode=" + accessCode +
                ", serverId=" + serverId);
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
        return serverId;
    }

    public AuthListener getAuthListener() {
        return authListener;
    }
}
