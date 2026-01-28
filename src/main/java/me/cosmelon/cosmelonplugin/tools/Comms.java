package me.cosmelon.cosmelonplugin.tools;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Basic class to enhance communication.
 */
public class Comms {
    /**
     * Send a message in both game chat and discord
     * @param msg
     */
    public static void send_global(String msg) {
        send_global(msg, ChatColor.WHITE);
    }

    /**
     * Send a message in both game chat and discord
     * @param msg
     * @param color
     */
    public static void send_global(String msg, ChatColor color) {
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), "**[SERVER]** " + msg) ;
        Bukkit.broadcastMessage(color + "» " + msg);
    }
}
