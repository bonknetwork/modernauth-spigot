package org.bonkmc.modernAuthentication;

import org.bukkit.plugin.java.JavaPlugin;

public final class ModernAuthentication extends JavaPlugin {

    private String backendUrl;
    private int backendPort;
    private String accessCode;
    private String serverId; // Loaded from config.
    private AuthListener authListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfiguration();

        authListener = new AuthListener(this);
        getServer().getPluginManager().registerEvents(authListener, this);

        getCommand("authreload").setExecutor(new ReloadCommand(this));
        // Register the confirmation command for switching flows.
        getCommand("modernconfirm").setExecutor(new ModernConfirmCommand(this));
    }

    @Override
    public void onDisable() {
        // Optional cleanup.
    }

    public void loadConfiguration() {
        backendUrl = getConfig().getString("backendUrl", "https://auth.bonkmc.org");
        backendPort = getConfig().getInt("backendPort", 8080);
        accessCode = getConfig().getString("access-code", "");
        serverId = getConfig().getString("server-id", "bonk-network");
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

    // New helper method to load a message from the config.
    public String getMessage(String key) {
        return getConfig().getString("messages." + key);
    }
}
