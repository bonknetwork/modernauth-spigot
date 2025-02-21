package org.bonkmc.modernAuthentication;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.nickuc.login.api.nLoginAPI;

public class AuthListener implements Listener {

    private final ModernAuthentication plugin;
    // Map to store scheduled authentication tasks for each player.
    private final Map<UUID, BukkitTask> authTasks = new HashMap<>();

    public AuthListener(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Skip if already authenticated.
        if (nLoginAPI.getApi().isAuthenticated(player.getName())) {
            return;
        }

        // Generate a unique token.
        String token = UUID.randomUUID().toString().replace("-", "");

        // Build the authentication URL using the public server ID.
        String authUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                "/auth/" + plugin.getServerId() + "/" + token +
                "?username=" + player.getName();
        // Optionally include the access code in the link if needed.
        String accessCode = plugin.getAccessCode();
        if (!accessCode.isEmpty()) {
            authUrl += "&access_code=" + accessCode;
        }

        // Create and send a clickable message.
        TextComponent clickableMessage = new TextComponent("§aClick here to authenticate");
        clickableMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, authUrl));
        clickableMessage.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to open the authentication link").create()
        ));
        player.spigot().sendMessage(clickableMessage);

        // Schedule a repeating asynchronous task to poll the backend API.
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                new AuthTask(plugin, this, player, token), 20L, 20L);
        authTasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
    }

    // Called by AuthTask to cancel a player's polling task.
    public void cancelAuthTask(UUID uuid) {
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
    }
}
