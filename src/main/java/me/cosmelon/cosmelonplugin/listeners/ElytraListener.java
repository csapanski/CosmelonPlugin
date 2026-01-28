package me.cosmelon.cosmelonplugin.listeners;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class ElytraListener implements Listener {

    CosmelonPlugin plugin;

    public ElytraListener(CosmelonPlugin plugin) {
        this.plugin = plugin;

        this.enabled = plugin.getConfig().getBoolean("terminal-velocity-enabled");
        // Convert from m/s to m/tick
        this.maxvel_mps = plugin.getConfig().getDouble("terminal-velocity-speed");
        this.maxvel_mpt = maxvel_mps/20.0;

        if (this.enabled) {
            Bukkit.getConsoleSender().sendMessage("Elytra veloc cap: " + maxvel_mps + " m/s.");
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);

    }


    // shamelessly copied from https://github.com/CosmicSubspace/CEN/blob/main/src/main/java/com/cosmicsubspace/mc/cen/ConfigurableElytraNerfs.java#L294

    // Terminal Velocity
    boolean enabled;
    double maxvel_mps;
    double maxvel_mpt;
    @EventHandler
    public void move(PlayerMoveEvent event) {
        Player p=event.getPlayer();
        Vector v=p.getVelocity();
        double speed = v.length();
        boolean gliding = p.isGliding();
        boolean overspeed=speed>maxvel_mpt;

        if (overspeed && gliding && enabled){
            p.setVelocity(v.normalize().multiply(maxvel_mpt));
//            if (rateLimitMsg("TermVel",p.getName(),10000)){
//                p.sendMessage(termvel_warn);
//            }
        }
    }
}
