package org.bonkmc.modernAuthentication;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final ModernAuthentication plugin;

    public ReloadCommand(ModernAuthentication plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.reloadConfig();
        plugin.loadConfiguration();
        sender.sendMessage(plugin.getMessage("reloadSuccess"));
        return true;
    }
}
