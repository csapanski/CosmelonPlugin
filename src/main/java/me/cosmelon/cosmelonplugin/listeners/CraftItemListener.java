package me.cosmelon.cosmelonplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftItemListener implements Listener {

    @EventHandler
    public void CraftItemEvent(CraftItemEvent event) {
        if (event.getWhoClicked().getScoreboardTags().contains("br_disablecrafting")) {
            event.setCancelled(true);
            Bukkit.getLogger().info("Player " + event.getWhoClicked().getName() + " has tag 'br_disablecrafting'");
        }
    }
}
