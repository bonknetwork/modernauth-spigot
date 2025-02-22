package org.bonkmc.modernAuthentication;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import com.nickuc.login.api.nLoginAPI;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class AuthTask implements Runnable {

    private final ModernAuthentication plugin;
    private final AuthListener authListener;
    private final Player player;
    private final String token;

    public AuthTask(ModernAuthentication plugin, AuthListener authListener, Player player, String token) {
        this.plugin = plugin;
        this.authListener = authListener;
        this.player = player;
        this.token = token;
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cancelTask();
            return;
        }

        try {
            // Build the API status URL using the public server ID.
            String statusUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                    "/api/authstatus/" + plugin.getServerId() + "/" + token;
            URL url = new URL(statusUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            // Use the access code when communicating with the backend.
            connection.setRequestProperty("X-Server-Secret", plugin.getAccessCode());

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                StringBuilder responseContent = new StringBuilder();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        responseContent.append(inputLine);
                    }
                }

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
        Bukkit.getScheduler().runTask(plugin, () -> authListener.cancelAuthTask(player.getUniqueId()));
    }
}
