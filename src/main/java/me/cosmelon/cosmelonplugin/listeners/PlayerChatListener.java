package me.cosmelon.cosmelonplugin.listeners;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    public PlayerChatListener(CosmelonPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    /**
     * Mute players with 'br_muted' tag
     */
    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();

        e.setFormat(player.getDisplayName() + ChatColor.RESET + ": " + ChatColor.WHITE + "%2$s");

        if (player.getScoreboardTags().contains("br_muted")) {
            if (player.isOp()) {
                return;
            }
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Chat is currently muted!");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.2f);
        }
    }
}
