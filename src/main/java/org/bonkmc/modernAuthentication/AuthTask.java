package org.bonkmc.modernAuthentication;

import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.Identity;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.SecureRandom;

public class AuthTask implements Runnable {

    private final ModernAuthentication plugin;
    private final AuthListener authListener;
    private final Player player;
    private final String token;
    // Indicates whether to change the user's password upon successful authentication.
    private final boolean changePassword;
    private final Identity identity;

    public AuthTask(ModernAuthentication plugin, AuthListener authListener, Player player, String token, boolean changePassword) {
        this.plugin = plugin;
        this.authListener = authListener;
        this.player = player;
        this.token = token;
        this.changePassword = changePassword;
        this.identity = Identity.ofKnownName(player.getName());
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
            // Use the access code in the request header (server-side only).
            connection.setRequestProperty("X-Server-Secret", plugin.getAccessCode());

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder responseContent = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    responseContent.append(inputLine);
                }
                in.close();

                if (responseContent.toString().contains("\"logged_in\":true")) {
                    // Force login on the main thread.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean success = nLoginAPI.getApi().forceLogin(identity, true);
                        if (success) {
                            player.sendMessage("§aAuthentication successful! You are now logged in.");
                            if (changePassword) {
                                String newPassword = generateRandomPassword();
                                String ip = player.getAddress() != null && player.getAddress().getAddress() != null ?
                                        player.getAddress().getAddress().getHostAddress() : "unknown";
                                // Attempt to register the player with the new password first.
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    boolean registered = nLoginAPI.getApi().performRegister(identity, newPassword);
                                    boolean passwordChanged = nLoginAPI.getApi().changePassword(identity, newPassword);
                                    if (passwordChanged) {
                                        plugin.getLogger().info("Player " + player.getName() + " registered with new password successfully.");
                                    } else {
                                        plugin.getLogger().warning("Failed to change password for " + player.getName());
                                    }
                                });
                            }
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

    // Generates a random password containing letters, numbers, and symbols.
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+[]{}|;:,.<>?";
        SecureRandom random = new SecureRandom();
        int length = 20 + random.nextInt(16); // Generates a length between 20 and 35.
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }
}
