package me.cosmelon.cosmelonplugin.commands;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Memory implements CommandExecutor {

    final private CosmelonPlugin plugin;
    public Memory(CosmelonPlugin plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        int num_args = strings.length;
        if (num_args != 1) {
            return false;
        }

        /** memory -usage */
        if (strings[0] == "-usage") {
            Runtime r = Runtime.getRuntime();
            long memused = (r.maxMemory() - r.freeMemory()) / 1048576;
            commandSender.sendMessage("Memory Usage: " + memused + "/" + r.maxMemory() / 1048576 + "MB");
        }

        return true;
    }
}
