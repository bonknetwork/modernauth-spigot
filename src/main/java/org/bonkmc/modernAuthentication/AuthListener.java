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
import java.util.Optional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.Identity;
import com.nickuc.login.api.types.AccountData;
import com.nickuc.login.api.event.bukkit.auth.LoginEvent;

public class AuthListener implements Listener {

    private final ModernAuthentication plugin;
    // Map to store scheduled authentication tasks for each player.
    private final Map<UUID, BukkitTask> authTasks = new HashMap<>();
    // Map to store players for whom confirmation should be delayed until login.
    private final Map<UUID, Boolean> delayedConfirmations = new HashMap<>();

    public AuthListener(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Create the Identity using the known name.
        Identity identity = Identity.ofKnownName(player.getName());

        // If already authenticated, skip the flow.
        if (nLoginAPI.getApi().isAuthenticated(identity)) {
            return;
        }

        // Asynchronously check if the user exists on the backend.
        String userCheckUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                "/api/isuser/" + plugin.getServerId() + "/" + player.getName();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(userCheckUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                int responseCode = connection.getResponseCode();
                StringBuilder response = new StringBuilder();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    // Parse the JSON response simply by checking for "exists":true.
                    boolean exists = response.toString().contains("\"exists\":true");
                    plugin.getLogger().info("User check for " + player.getName() + ": " + response.toString());
                    // Continue on the main thread.
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (exists) {
                            // Existing users get the normal login flow immediately.
                            startAuthentication(player, false);
                        } else {
                            // For HTTP "exists" false, check the nLogin account.
                            Optional<AccountData> accountOpt = nLoginAPI.getApi().getAccount(identity);
                            if (accountOpt.isPresent()) {
                                // Player is registered with nLogin – delay confirmation until after login.
                                delayedConfirmations.put(player.getUniqueId(), true);
                            } else {
                                // Player is not registered – send the confirmation immediately.
                                sendSwitchingConfirmation(player);
                            }
                        }
                    });
                } else {
                    plugin.getLogger().warning("User check API responded with code " + responseCode + " for " + player.getName());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error while checking user for " + player.getName() + ": " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (authTasks.containsKey(uuid)) {
            authTasks.get(uuid).cancel();
            authTasks.remove(uuid);
        }
        delayedConfirmations.remove(uuid);
    }

    @EventHandler
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (delayedConfirmations.containsKey(player.getUniqueId())) {
            sendSwitchingConfirmation(player);
            delayedConfirmations.remove(player.getUniqueId());
        }
    }

    // Starts the authentication flow.
    // changePassword: if true, once authenticated the player's password will be changed.
    public void startAuthentication(Player player, boolean changePassword) {
        // Generate a unique token.
        String token = UUID.randomUUID().toString().replace("-", "");

        // Build the authentication URL WITHOUT including the access token.
        String authUrl = plugin.getBackendUrl() + ":" + plugin.getBackendPort() +
                "/auth/" + plugin.getServerId() + "/" + token +
                "?username=" + player.getName();

        // Build the formatted clickable message.
        // This message indicates that password login is disabled and prompts the user to click to login.
        String headerFooter = "§7------------------------------";
        String messageBody = "§cPassword login is disabled for this account.\n" +
                "§aClick here to login using ModernAuth.";
        TextComponent fullMessage = new TextComponent(headerFooter + "\n" + messageBody + "\n" + headerFooter);
        fullMessage.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, authUrl));
        fullMessage.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Open the ModernAuth login page").create()
        ));
        player.spigot().sendMessage(fullMessage);

        // Schedule a repeating asynchronous task to poll the backend API.
        BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                new AuthTask(plugin, this, player, token, changePassword),
                20L, 20L);
        authTasks.put(player.getUniqueId(), task);
    }

    // Sends the confirmation message for switching to the new authentication system
    // as a clickable chat message.
    public void sendSwitchingConfirmation(Player player) {
        String message = "§eClick here to switch to ModernAuth";
        TextComponent confirmMessage = new TextComponent(message);
        confirmMessage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/modernconfirm yes"));
        confirmMessage.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Switch to ModernAuth").create()
        ));
        player.spigot().sendMessage(confirmMessage);
    }

    public void cancelAuthTask(UUID uuid) {
        authTasks.remove(uuid);
    }
}
