package me.cosmelon.cosmelonplugin.commands;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Sin implements CommandExecutor, TabCompleter {

    private CosmelonPlugin plugin;
    private final boolean sin_active;
    final String[] sins = {"envy", "gluttony", "greed", "pride", "lust", "sloth", "wrath"};

    public Sin(CosmelonPlugin plugin) {
        this.plugin = plugin;
        this.sin_active = plugin.getConfig().getBoolean("sins");

        if (sin_active) {
            return;
        }

        // create teams if they don't already exist
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String sin : sins) {
            final String teamname = "sin." + sin;

            if (board.getTeam(teamname) == null) {
                board.registerNewTeam("sin." + sin);
                Team t = board.getTeam("sin." + sin);
                t.setPrefix("[" + sin.substring(0,1).toUpperCase() + sin.substring(1) + "] ");
            }
        }
    }



    /**
     * Mark a player with their sin.
     * /sin <type> <player>
     *
     * type can be either of ["envy", "gluttony", "greed", "pride", "lust", "sloth", "wrath"]
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (sin_active) {
            sender.sendMessage(ChatColor.RED + "Sin is disabled at the moment.");
            return false;
        }

        int num_args = args.length;
        if (num_args != 2) {
            sender.sendMessage(ChatColor.RED + "Invalid arguments!");
            return false;
        }

        // prevent self reporting
        if (sender instanceof Player
                && args[1].equals(sender.getName())
                && !sender.hasPermission("CosmelonPlugin.sinreset")) {
            sender.sendMessage(ChatColor.RED + "No self reporting!");
            return false;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found or offline.");
            return false;
        }

        // identify sin
        String sin = args[0];
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        if (sender.hasPermission("CosmelonPlugin.sinreset") && args[0].equals("-reset")) {
            for (String sin_type : sins) {
                Team t = board.getTeam("sin." + sin_type);
                if (t != null && t.hasEntry(target.getName())) {
                    t.removeEntry(target.getName());
                    sender.sendMessage(ChatColor.GREEN + "Sin reset for " + target.getName());
                    return true;
                }
            }
            sender.sendMessage("Player " + target.getName() + " is not assigned to any sin.");
            return true;
        }

        Team target_sin = board.getTeam("sin." + sin);

        if (target_sin != null) {
            target_sin.addEntry(target.getName());
            plugin.send_global(target.getName() + " is now " + sin + ".", ChatColor.DARK_RED);
            return true;
        } else {
            sender.sendMessage("Invalid sin! Try a different one!");
            return false;
        }
    }


    // Tab completion vibed via ChatGPT
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest sin types
            for (String sin : sins) {
                if (sin.startsWith(args[0].toLowerCase())) {
                    completions.add(sin);
                }
            }
            // Also allow the reset option if sender has permission
            if (sender.hasPermission("CosmelonPlugin.sinreset") && "-reset".startsWith(args[0].toLowerCase())) {
                completions.add("-reset");
            }
        } else if (args.length == 2) {
            // Suggest online player names
            for (Player p : sender.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }


}
