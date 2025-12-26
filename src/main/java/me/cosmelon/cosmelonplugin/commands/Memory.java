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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int num_args = args.length;
        if (num_args != 1) {
            return false;
        }

        /** memory -usage */
        if (args[0].equalsIgnoreCase("-usage")) {
            Runtime r = Runtime.getRuntime();
            long memused = (r.maxMemory() - r.freeMemory()) / 1048576;
            sender.sendMessage("Memory Usage: " + memused + "/" + r.maxMemory() / 1048576 + "MB");
        }

        return true;
    }
}
