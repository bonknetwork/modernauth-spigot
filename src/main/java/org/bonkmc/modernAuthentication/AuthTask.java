package org.bonkmc.modernAuthentication;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.nickuc.login.api.nLoginAPI;

public class AuthTask implements Runnable {

    private final ModernAuthentication plugin;
    private final AuthListener authListener; // Reference to AuthListener
    private final Player player;
    private final String token;
    private final long startTime;
    private final int authTimeoutMillis;

    public AuthTask(ModernAuthentication plugin, AuthListener authListener, Player player, String token) {
        this.plugin = plugin;
        this.authListener = authListener;
        this.player = player;
        this.token = token;
        this.startTime = System.currentTimeMillis();
        this.authTimeoutMillis = plugin.getAuthTimeout() * 1000;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancelTask();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > authTimeoutMillis) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage("§cAuthentication timed out. Please rejoin to try again.");
                player.kickPlayer("Authentication timed out.");
            });
            cancelTask();
            return;
        }

        try {
            String statusUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() + "/api/authstatus/" + token;
            URL url = new URL(statusUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder responseContent = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseContent.append(inputLine);
                }
                in.close();

                if (responseContent.toString().contains("\"logged_in\":true")) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean success = nLoginAPI.getApi().forceLogin(player.getName(), true);
                        if (success) {
                            player.sendMessage("§aAuthentication successful! You are now logged in.");
                        } else {
                            player.sendMessage("§cAuthentication succeeded, but login failed. Please contact an admin.");
                        }
                    });
                    cancelTask();
                    return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error while checking auth status for " + player.getName() + ": " + e.getMessage());
        }
    }

    private void cancelTask() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            authListener.cancelAuthTask(player.getUniqueId());
        });
    }
}
