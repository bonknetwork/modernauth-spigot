package org.bonkmc.modernAuthentication;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ModernConfirmCommand implements CommandExecutor {

    private final ModernAuthentication plugin;

    public ModernConfirmCommand(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("notPlayer"));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(plugin.getMessage("confirmUsage"));
            return true;
        }
        String response = args[0].toLowerCase();
        if (response.equals("yes")) {
            // Start the authentication flow with changePassword enabled (registration/switching flow).
            plugin.getAuthListener().startAuthentication(player, true);
        } else if (response.equals("no")) {
            player.sendMessage(plugin.getMessage("noSwitch"));
        } else {
            player.sendMessage(plugin.getMessage("invalidOption"));
        }
        return true;
    }
}
