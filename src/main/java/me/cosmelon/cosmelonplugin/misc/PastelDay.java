package me.cosmelon.cosmelonplugin.misc;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import me.cosmelon.cosmelonplugin.ServerResourceHandler;
import me.cosmelon.cosmelonplugin.tools.Comms;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static java.lang.Math.random;

public class PastelDay {

    private final ServerResourceHandler handler;
    private final boolean pastel_day_allowed;
    private long last_time = -1;

    public PastelDay(ServerResourceHandler handler, CosmelonPlugin plugin) {
        this.handler = handler;
        this.pastel_day_allowed = plugin.getConfig().getBoolean("pastel-day");

        Bukkit.getLogger().warning("PastelDay constructed");

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long time = Bukkit.getWorld("world").getTime();

            if (time < last_time) {
                // new minecraft day has started
                // remove billy day
                if (pastel_day) {
                    pastel_day = false;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(ChatColor.AQUA +"BILLY DAY OVER", "WE KILLED HIM");
                        handler.applyresources(p);
                    }
                    Comms.send_global("Billy day ended!", ChatColor.AQUA);
                }

                // try for billy day
                if (pastel_day_allowed && !pastel_day) rollForPastel();
            }
            last_time = time;
        }, 0L, 20L);
    }

    private boolean pastel_day = false;

    public boolean getPastelStatus() {
        return pastel_day;
    }

    private void rollForPastel() {
        // want at least a few players online for it
        if (Bukkit.getOnlinePlayers().size() < 3) {
            return;
        }

        double rand = random();
        if (rand < 0.1) {
            // billy event happening
            pastel_day = true;
            Comms.send_global("Billy day starting now!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setTitleTimes(0, 4, 1);
                p.sendTitle(ChatColor.AQUA +"BILLY DAY!!!!!", "");
                handler.applyresources(p);
            }
        } else {
            Bukkit.getLogger().info("PastelDay rolled " + rand);
        }
    }
}
