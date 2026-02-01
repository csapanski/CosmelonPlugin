package me.cosmelon.cosmelonplugin.listeners;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public class OnInteractAtEntity implements Listener {

    private final CosmelonPlugin plugin;
    public OnInteractAtEntity(CosmelonPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void OnInteractAtEntity(PlayerInteractAtEntityEvent e) {
        boolean bigrat = plugin.getConfig().getBoolean("bigrat");
        if (e.getRightClicked() instanceof ArmorStand && bigrat) {
            if (e.getPlayer().getScoreboardTags().contains("debug")) {
                e.getPlayer().sendMessage("[Debug] Armor stand clicked. Event ignored.");
            }
            e.setCancelled(true);
        }
    }
}
