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

// Import the nLogin API (ensure the dependency is on your build path)
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

        // If the player is already authenticated via nLogin, do nothing.
        if (nLoginAPI.getApi().isAuthenticated(player.getName())) {
            return;
        }

        // Generate a unique token (without dashes)
        String token = UUID.randomUUID().toString().replace("-", "");

        // Construct the authentication URL:
        // e.g. http://127.0.0.1:3000/auth/{token}?username=PlayerName
        String authUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() + "/auth/"
                + token + "?username=" + player.getName();

        // Create a clickable message using Spigot's TextComponent.
        TextComponent clickableMessage = new TextComponent("§aClick here to authenticate");
        clickableMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, authUrl));
        clickableMessage.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Click to open the authentication link").create()
        ));
        // Send the clickable message to the player.
        player.spigot().sendMessage(clickableMessage);

        // Schedule an asynchronous repeating task (AuthTask) to poll your backend.
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

    // Called by AuthTask to cancel a player's authentication task.
    public void cancelAuthTask(UUID uuid) {
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
    }
}
