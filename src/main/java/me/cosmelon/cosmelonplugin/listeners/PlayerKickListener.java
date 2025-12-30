package me.cosmelon.cosmelonplugin.listeners;

import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import me.cosmelon.cosmelonplugin.PlayerID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerKickListener implements Listener {

    private final CosmelonPlugin plugin;

    public PlayerKickListener(CosmelonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void on_command(ServerCommandEvent event) {
        // kick from the console
        String msg = event.getCommand();
        command(msg);
    }

    @EventHandler
    public void on_command(PlayerCommandPreprocessEvent event) {
        // kick from a player
        String msg = event.getMessage();
        if (msg.startsWith("/kick ")) {
            command(msg.substring(1));
        }
    }


    private Set<UUID> manually_kicked = new HashSet<>();
    @EventHandler
    public void on_kick(PlayerKickEvent event) {
        Player p = event.getPlayer();
        String reason = event.getReason();

        // make sure this isn't sent when autokick or when bigrat is on
        if (!manually_kicked.contains(p.getUniqueId()) || plugin.bigrat) return;

        manually_kicked.remove(p.getUniqueId());

        // determine the message and send it
        if (!reason.isEmpty()) {
            plugin.send_global(p.getName() + " was kicked for: " + reason, ChatColor.RED);
        } else {
            plugin.send_global("Kicked " + p.getName(), ChatColor.RED);
        }
    }


    // general command handler for the trigger
    private void command(String msg) {
        if (msg.startsWith("kick ")) {
            String[] args = msg.split(" ");
            if (2 <= args.length) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target != null) {
                    manually_kicked.add(target.getUniqueId());
                }
            }
        }
    }
}
