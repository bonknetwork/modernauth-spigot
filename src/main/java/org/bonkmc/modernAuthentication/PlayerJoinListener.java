package org.bonkmc.modernAuthentication;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PlayerJoinListener implements Listener {
    private final ModernAuthentication plugin;

    public PlayerJoinListener(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        boolean firstTime = isFirstTime(uuid);
        String token = getOrCreateToken(uuid);
        String url;
        // For first-time sign-up, include the Minecraft username as a query parameter.
        if (firstTime) {
            url = "http://10.1.1.116:3000/auth/" + token + "?username=" + player.getName();
        } else {
            // For subsequent logins, omit the username.
            url = "http://10.1.1.116:3000/auth/" + token;
        }

        // Build a clickable chat message.
        TextComponent message = new TextComponent("Click here to " + (firstTime ? "sign up" : "log in") + ": ");
        TextComponent urlComponent = new TextComponent(url);
        urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        message.addExtra(urlComponent);

        player.spigot().sendMessage(message);
    }

    private boolean isFirstTime(String uuid) {
        try {
            Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT token FROM players WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            boolean firstTime = !rs.next();
            rs.close();
            ps.close();
            return firstTime;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getOrCreateToken(String uuid) {
        try {
            Connection conn = plugin.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT token FROM players WHERE uuid = ?");
            ps.setString(1, uuid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String token = rs.getString("token");
                rs.close();
                ps.close();
                return token;
            }
            rs.close();
            ps.close();

            // No token exists; create a new one.
            String token = java.util.UUID.randomUUID().toString();
            PreparedStatement insert = conn.prepareStatement("INSERT INTO players (uuid, token, authenticated) VALUES (?, ?, 0)");
            insert.setString(1, uuid);
            insert.setString(2, token);
            insert.executeUpdate();
            insert.close();
            return token;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "error-token";
    }
}
