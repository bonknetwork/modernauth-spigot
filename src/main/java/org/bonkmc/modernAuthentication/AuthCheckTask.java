package org.bonkmc.modernAuthentication;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthCheckTask extends BukkitRunnable {
    private final ModernAuthentication plugin;
    // Base URL for the Python server API endpoint.
    private final String apiBaseUrl = "http://10.1.1.116:3000/api/authstatus/";

    public AuthCheckTask(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        // Iterate over a snapshot of online players.
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            String uuid = player.getUniqueId().toString();
            try {
                Connection conn = plugin.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT token, authenticated FROM players WHERE uuid = ?");
                ps.setString(1, uuid);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String token = rs.getString("token");
                    int localStatus = rs.getInt("authenticated");
                    // Only check if the player has not yet been marked as logged in.
                    if (localStatus == 0) {
                        boolean remoteLoggedIn = checkAuthStatus(token);
                        if (remoteLoggedIn) {
                            // Schedule a synchronous task to send the message and update the DB.
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                player.sendMessage("You have been logged in via the website!");
                                try {
                                    Connection conn2 = plugin.getConnection();
                                    PreparedStatement update = conn2.prepareStatement("UPDATE players SET authenticated = 1 WHERE uuid = ?");
                                    update.setString(1, uuid);
                                    update.executeUpdate();
                                    update.close();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
                rs.close();
                ps.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkAuthStatus(String token) {
        try {
            URL url = new URL(apiBaseUrl + token);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            // Set timeouts to avoid hanging indefinitely.
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // Expect a JSON response like: {"logged_in": true}
                return response.toString().contains("\"logged_in\":true");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
