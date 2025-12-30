package me.cosmelon.cosmelonplugin.misc;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import static java.lang.Math.random;

public class PastelDay implements Listener {

    private final CosmelonPlugin plugin;
    private final boolean pastel_day_allowed;
    private long last_time = -1;

    public PastelDay(CosmelonPlugin plugin) {
        this.plugin = plugin;
        this.pastel_day_allowed = plugin.getConfig().getBoolean("pastel-day");

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long time = Bukkit.getWorld("world").getTime();

            if (time < last_time) {
                // new minecraft day has started
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
        if (Bukkit.getOnlinePlayers().size() > 2) {
            return;
        }

        if (random() < 0.05) {
            plugin.send_global("Billy day starting now!");
            pastel_day = true;
        }
    }


}
