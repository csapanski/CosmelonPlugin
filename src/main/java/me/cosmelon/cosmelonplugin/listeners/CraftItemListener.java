package me.cosmelon.cosmelonplugin.listeners;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;

public class CraftItemListener implements Listener {

    private final CosmelonPlugin plugin;
    public CraftItemListener(CosmelonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void CraftItemEvent(CraftItemEvent event) {
        if (event.getWhoClicked().getScoreboardTags().contains("br_disablecrafting")) {
            event.setCancelled(true);
        }
    }
}
