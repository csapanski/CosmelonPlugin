package me.cosmelon.cosmelonplugin;


import java.util.ArrayList;
import java.util.UUID;
import mc.obliviate.inventory.InventoryAPI;
import me.cosmelon.cosmelonplugin.listeners.CompassClickListener;
import me.cosmelon.cosmelonplugin.listeners.EntityDamageByEntityEventListener;
import me.cosmelon.cosmelonplugin.listeners.OnInteractAtEntity;
import me.cosmelon.cosmelonplugin.listeners.PlayerChatListener;
import me.cosmelon.cosmelonplugin.listeners.ResourceDenyListener;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class CosmelonPlugin extends JavaPlugin implements Listener {
    DataManager manager;
    ServerResourceHandler serverResourceHandler;
    Whitelist whitelist;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getConsoleSender().sendMessage("CosmelonPlugin Enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        new ResourceDenyListener(this);
        new CompassClickListener(this);
        new EntityDamageByEntityEventListener(this);
        new PlayerChatListener(this);
        new OnInteractAtEntity(this);

        startTagCheckTask();
        startPlayerCountTask();
        new InventoryAPI(this).init();

        manager = new DataManager(this);
        whitelist = new Whitelist(this);
        //webHandler = new WebHandler("mainpack", 5578, "/web/bigrat_beta.zip", this);
        serverResourceHandler = new ServerResourceHandler(this);


        // remove collisionRule_?? team
        collidefix();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage("CosmelonPlugin Disabled");
        serverResourceHandler.stop();
    }

    /**
     * Set conditions on join to ensure accuracy
     *
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        applyresources(player);

        if (player.getScoreboardTags().contains("bp_alive")) {
            hideAllPlayers(player);
        }

        ArrayList<String> downResourses = new ArrayList<>();
        for (WebHandler resourceServer : serverResourceHandler.server_list) {
            if (!resourceServer.online) {
                downResourses.add(resourceServer.packname);
            }
        }
        //player.sendMessage(ChatColor.AQUA + "Resource pack service is currently unreliable. Working on fixing it.");
        if (downResourses.size() > 0) {
            player.sendMessage(ChatColor.AQUA + "Resource pack service is currently unavailable for:");
            for (String packname : downResourses) {
                player.sendMessage(ChatColor.AQUA + packname);
            }
        }

        player.removeScoreboardTag("br_muted");
        player.setPlayerListHeader("\n\n\n\n\n\n\n\n\n\n\n\uE009");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        showAllPlayers(player);
    }

    public void showAllPlayers(Player player) {
        for (Player other_players : Bukkit.getOnlinePlayers()) {
            player.showPlayer(this, other_players);
        }
    }

    public void hideAllPlayers(Player player) {
        for (Player other_players : Bukkit.getOnlinePlayers()) {
            player.hidePlayer(this, other_players);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // ---- player-specific commands ----
        if (sender instanceof Player) {
            Player player = (Player) sender;

            /** /playerhide /playershow manual command */
            if (cmd.getName().equalsIgnoreCase("playerhide")) {
                hideAllPlayers(player);
                return true;
            } else if (cmd.getName().equalsIgnoreCase("playershow")) {
                showAllPlayers(player);
                return true;
            }
            /** colorblind resource pack request */
            // /colorblind
            if (cmd.getName().equalsIgnoreCase("colorblind")) {
                if (!player.getScoreboardTags().contains("br_colorblind")) {
                    player.addScoreboardTag("br_colorblind");
                } else {
                    player.removeScoreboardTag("br_colorblind");
                }
                applyresources(player);
                return true;
            }
            /** load a fresh copy of the resource pack */
            if (cmd.getName().equalsIgnoreCase("reloadpack")) {
                applyresources(player);
                return true;
            }
            /** readycheck **/
            if (cmd.getName().equalsIgnoreCase("reafycheck")) {
                int count = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getScoreboardTags().contains("player")) {
                        count++;
                    }
                }
                if (count == 0) {
                    return false;
                }

                Bukkit.dispatchCommand(sender,"function main:lobby/ready/readycheck");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getScoreboardTags().contains("player")) {
                        new ReadyCheck(p).open();
                    }
                }
                return true;
            }
        }

        // ---- general commands ----
        /** custom whitelist implementation */
        if(cmd.getName().equalsIgnoreCase("whitelist")) {
            // there is no security issue because CosmelonPlugin.whitelist
            whitelist.whitelistcmd(sender, cmd, label, args);
            return true;
        }
        /** memusage */
        if(cmd.getName().equalsIgnoreCase("memusage")) {
            Runtime r = Runtime.getRuntime();
            long memused = (r.maxMemory() - r.freeMemory()) / 1048576;
            sender.sendMessage("Memory Usage: " + memused + "/" + r.maxMemory() / 1048576 + "MB");
            return true;
        }

        return false;
   }

    // ------- PRIVATE METHODS -------
    private void startTagCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getScoreboardTags().contains("bp_alive")) {
                        // if the player is alive AND in pre-game
                        if (player.getScoreboardTags().contains("bp_pregame")) {
                            showAllPlayers(player);
                            continue;
                        }
                        hideAllPlayers(player);
                    } else if (player.getScoreboardTags().contains("bp_dead")) {
                        showAllPlayers(player);
                    }

                    if (!player.getScoreboardTags().contains("player")) {
                        showAllPlayers(player);
                    }

                    if (player.getScoreboardTags().contains("force_show_players")) {
                        showAllPlayers(player);
                    }

                    if (player.getScoreboardTags().contains("uncolorblind") && player.getScoreboardTags().contains("br_colorblind")) {
                        player.removeResourcePacks();
                        player.setResourcePack(serverResourceHandler.getUrl("mainpack"));

                        player.removeScoreboardTag("br_colorblind");
                        player.removeScoreboardTag("uncolorblind");
                        return;
                    }
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    // send the proper resource pack to a player
    private void applyresources(Player player) {
        if (player.getScoreboardTags().contains("br_colorblind")) {

            //player.addResourcePack(UUID ,serverResourceHandler.getUrl("colorblind"));
            player.addResourcePack(UUID.randomUUID(), serverResourceHandler.getUrl("colorblind"), null, "Install accessibility pack?", true);
        } else {
            player.removeResourcePacks();
            player.setResourcePack(serverResourceHandler.getUrl("mainpack"));
        }

    }

    private void startPlayerCountTask() {
        new BukkitRunnable() {

            @Override
            public void run() {

                int count = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getScoreboardTags().contains("player")) {
                        count++;
                    }
                }

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setPlayerListFooter(
                        net.md_5.bungee.api.ChatColor.of("#1cc9bb") + "Players: " + count + "/16");
                }
            }
        }.runTaskTimerAsynchronously(this, 10L, 10L);
    }

    private void collidefix() {
        // collideRule_<num> hack fix
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            String teamName = team.getName();

            if (teamName.startsWith("collideRule_")) {
                team.unregister();
                Bukkit.getConsoleSender().sendMessage(teamName + " removed. This is a hack fix --Cosmelon.");
            }
        }
    }

    @EventHandler
    public void CraftItemEvent(CraftItemEvent event) {
        if (event.getWhoClicked().getScoreboardTags().contains("br_disablecrafting")) {
            event.setCancelled(true);
        }
    }

}