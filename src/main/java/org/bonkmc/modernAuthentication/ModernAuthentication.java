package org.bonkmc.modernAuthentication;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

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

        // ─── VELOCITY DETECTION VIA paper-global.yml ───
        // Look for the global Paper config one level above plugins/, under config/paper-global.yml
        File serverRoot = getDataFolder().getParentFile().getParentFile();
        File globalConfig = new File(serverRoot, "config/paper-global.yml");
        if (globalConfig.exists()) {
            YamlConfiguration paper = YamlConfiguration.loadConfiguration(globalConfig);
            boolean velEnabled = paper.getBoolean("proxies.velocity.enabled", false);
            if (velEnabled) {
                getLogger().severe("======================================================");
                getLogger().severe("Detected Velocity proxy (proxies.velocity.enabled=true) in paper-global.yml");
                getLogger().severe("This plugin is not supported behind a Velocity proxy.");
                getLogger().severe("Disabling ModernAuthentication.");
                getLogger().severe("======================================================");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }

        // ─── NORMAL STARTUP ───
        authListener = new AuthListener(this);
        getServer().getPluginManager().registerEvents(authListener, this);

        getCommand("authreload").setExecutor(new ReloadCommand(this));
        getCommand("modernconfirm").setExecutor(new ModernConfirmCommand(this));
    }

    @Override
    public void onDisable() {
        // Optional cleanup.
    }

    public void loadConfiguration() {
        backendUrl   = getConfig().getString("backendUrl",  "https://auth.bonkmc.org");
        backendPort  = getConfig().getInt("backendPort",    8080);
        accessCode   = getConfig().getString("access-code", "");
        serverId     = getConfig().getString("server-id",   "bonk-network");
    }

    public String getBackendUrl()           { return backendUrl; }
    public int    getBackendPort()          { return backendPort; }
    public String getAccessCode()           { return accessCode; }
    public String getServerId()             { return serverId; }
    public AuthListener getAuthListener()   { return authListener; }

    // Helper to load localized messages from config
    public String getMessage(String key) {
        return getConfig().getString("messages." + key);
    }
}
