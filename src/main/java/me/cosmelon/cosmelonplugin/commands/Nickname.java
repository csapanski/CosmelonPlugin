package me.cosmelon.cosmelonplugin.commands;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/**
 * Set a nickname for the specified player.
 * /nick <target> <name>
 * /nick <target> -reset
 */
public class Nickname implements CommandExecutor {

    final private CosmelonPlugin plugin;
    private FileConfiguration config_file;
    public Nickname(CosmelonPlugin plugin) {
        this.plugin = plugin;
        this.config_file = plugin.getConfig();

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) return false;

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) return false;

        String nick = "";
        if (args.length == 3 && args[2] == "-reset") {
            nick = target.getName();
        } else {
            nick = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        target.setDisplayName(nick);
        target.setPlayerListName(nick);

        target.sendMessage(ChatColor.GOLD + "Your name is now set to " + nick + ".");
        return true;
    }

}
