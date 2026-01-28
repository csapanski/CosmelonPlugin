package me.cosmelon.cosmelonplugin;


import java.util.ArrayList;
import java.util.UUID;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import mc.obliviate.inventory.InventoryAPI;
import me.cosmelon.cosmelonplugin.commands.Memory;
import me.cosmelon.cosmelonplugin.commands.Nickname;
import me.cosmelon.cosmelonplugin.commands.Sin;
import me.cosmelon.cosmelonplugin.listeners.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class CosmelonPlugin extends JavaPlugin implements Listener {
    public DataManager manager;
    ServerResourceHandler serverResourceHandler;
    Whitelist whitelist;
    public boolean bigrat;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getConsoleSender().sendMessage("CosmelonPlugin Enabled!");
        getServer().getPluginManager().registerEvents(this, this);
        init();

        // remove collisionRule_?? team
        collidefix();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Bukkit.getConsoleSender().sendMessage("CosmelonPlugin Disabled");
        serverResourceHandler.stop();
    }

    public void init() {
        new ResourceDenyListener(this);
        new CompassClickListener(this);
        new EntityDamageByEntityEventListener(this);
        new PlayerChatListener(this);
        new OnInteractAtEntity(this);
        new InventoryAPI(this).init();

        manager = new DataManager(this);
        whitelist = new Whitelist(this);
        serverResourceHandler = new ServerResourceHandler(this);

        // get if on bigrat server
        bigrat = getConfig().getBoolean("bigrat");

        if (bigrat) startTagCheckTask();
        startPlayerCountTask();

        if (!bigrat) new ElytraListener(this);

        // /nick command
        Nickname nickManager = new Nickname(this);
        getCommand("nick").setExecutor(nickManager);
        getServer().getPluginManager().registerEvents(nickManager, this);

        getCommand("memory").setExecutor(new Memory(this));

        // kick listener
        PlayerKickListener kickListener = new PlayerKickListener(this);
        getServer().getPluginManager().registerEvents(kickListener, this);

        // sinning!
        Sin sin = new Sin(this);
        getCommand("sin").setExecutor(sin);
    }


    public void reloadPlugin(CommandSender sender) {
        Bukkit.getScheduler().cancelTasks(this);

        if (serverResourceHandler != null) {
            serverResourceHandler.stop();
        }

        manager = null;
        whitelist = null;
    }


    /**
     * Set conditions on join to ensure accuracy
     *
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        serverResourceHandler.applyresources(player);

        if (player.getScoreboardTags().contains("bp_alive") && bigrat) {
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
        if (bigrat) player.setPlayerListHeader("\n\n\n\n\n\n\n\n\n\n\n\uE009");
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
            if (cmd.getName().equalsIgnoreCase("colorblind") && bigrat) {
                if (!player.getScoreboardTags().contains("br_colorblind")) {
                    player.addScoreboardTag("br_colorblind");
                } else {
                    player.removeScoreboardTag("br_colorblind");
                }
                serverResourceHandler.applyresources(player);
                return true;
            }
            /** load a fresh copy of the resource pack */
            if (cmd.getName().equalsIgnoreCase("reloadpack")) {
                int num_args = args.length;
                if (num_args == 1 && player.isOp()) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().equalsIgnoreCase(args[0])) {
                            serverResourceHandler.applyresources(p);
                            return true;
                        }
                    }
                }
                serverResourceHandler.applyresources(player);
                return true;
            }
            /** readycheck **/
            if (cmd.getName().equalsIgnoreCase("reafycheck") && bigrat) {
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

        return false;
   }


   /**
    * Send a message in both game chat and discord
    */
    public void send_global(String msg) {
       send_global(msg, ChatColor.WHITE);
   }

    public void send_global(String msg, ChatColor color) {
        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), "**[SERVER]** " + msg) ;
        Bukkit.broadcastMessage(color + "» " + msg);
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



    private void startPlayerCountTask() {
        new BukkitRunnable() {

            @Override
            public void run() {
                int count = 0;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!bigrat || player.getScoreboardTags().contains("player")) {
                        count++;
                    }
                }
                int max_players = Bukkit.getMaxPlayers();
                if (bigrat) max_players = 16;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setPlayerListFooter(
                        net.md_5.bungee.api.ChatColor.of("#1cc9bb") + "Players: " + count + "/" + max_players);
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

    protected boolean bigratStatus() {
        return this.bigrat;
    }





}