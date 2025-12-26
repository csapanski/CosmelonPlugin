package me.cosmelon.cosmelonplugin.commands;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

/**
 * Set a nickname for the specified player.
 * /nick <target> <name>
 * /nick <target> -reset
 */
public class Nickname implements CommandExecutor, Listener {

    private final CosmelonPlugin plugin;
    private final Map<UUID, String> nicknames = new HashMap<>();

    public Nickname(CosmelonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) return false;

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) return false;

        String nick = "";
        if (args.length == 3 && args[2].equalsIgnoreCase("-reset")) {
            nick = target.getName();
            nicknames.remove(target.getUniqueId());
        } else {
            nick = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            nicknames.put(target.getUniqueId(), nick);
        }

        applyNick(target, nick);

        Bukkit.broadcastMessage(ChatColor.GOLD + "» " + target.getName() + " is now " + nick + ".");
        sender.sendMessage(ChatColor.GOLD + "» " + target.getName() + " is now " + nick + ".");
        Bukkit.getLogger().info( sender.getName() + " nicked " + target.getName() + " as " + nick + ".");
        return true;
    }

    public void applyNick(Player player, String nick) {
        // Chat and tab
        player.setDisplayName(nick);
        player.setPlayerListName(nick);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        String nick = nicknames.get(p.getUniqueId());
        if (nick != null) {
            applyNick(p, nick);
        }
    }
}
