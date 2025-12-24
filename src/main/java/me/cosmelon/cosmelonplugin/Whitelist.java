package me.cosmelon.cosmelonplugin;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class Whitelist implements Listener {
    private CosmelonPlugin plugin;
    private FileConfiguration config_file;
    private boolean enforce_whitelist;
    Whitelist(CosmelonPlugin plugin) {
        this.plugin = plugin;
        this.config_file = plugin.manager.getConfig();
        this.enforce_whitelist = config_file.getBoolean("whitelist.enforce");
        Bukkit.getPluginManager().registerEvents(this, plugin);
        configure();
    }

    final String invalid_args_msg = ChatColor.RED + "Invalid arguments!";
    void whitelistcmd(CommandSender sender, Command cmd, String label, String[] args) {
        final int num_args = args.length;
        if (num_args == 0) {
            sender.sendMessage(invalid_args_msg);
            return;
        }

        if (lists.contains(args[0]) && (num_args == 2)) {
            switch (args[1]) {
            case "add":
                // add the list
                sender.sendMessage(add_list(args[0]));
                break;
            case "remove":
                // remove the list
                sender.sendMessage(rem_list(args[0]));
                break;
            case "list":
                // list player names on specified list
            default:
                sender.sendMessage(invalid_args_msg);
                break;
            }
        } else if (args[0].equalsIgnoreCase("list") && num_args == 1) {
            StringBuilder temp = new StringBuilder();
            temp.append(active_lists.size() + " group(s) active: ");
            for (int i = 0; i < active_lists.size(); i++) {
                temp.append(active_lists.get(i) + ", ");
            }
            sender.sendMessage(temp.toString());

            temp.delete(0,temp.length());
            temp.append(lists.size() + " group(s) available: ");
            for (int i = 0; i < lists.size(); i++) {
                temp.append(lists.get(i) + ", ");
            }
            sender.sendMessage(temp.toString());

        } else if (args[0].equalsIgnoreCase("tempplayer") && num_args == 2) {
            // usage: /whitelist tempplayer <name>
            //add_temp(args[1]);
            if (args[1].equalsIgnoreCase("-list")) {
                String s = "";
                for (String name : temp_player_names) {
                    s += name + ", ";
                }
                sender.sendMessage("Temp players: "_+ s);
                return;
            }

//            sender.sendMessage(ChatColor.AQUA + "This feature is currently WIP. Use /whitelist off for now and turn it back on when done.");
            if (temp_player_names.contains(args[1])) {
                sender.sendMessage(args[1] + " removed from temp whitelist.");
                temp_player_names.remove(args[1]);
                return;
            }
            temp_player_names.add(args[1]);
            sender.sendMessage(args[1] + " added to temp whitelist.");

        } else if (args[0].equals("on")) {
            // enforce the whitelist
            if (enforce_whitelist) {
                sender.sendMessage(ChatColor.GREEN + "Whitelist is already on!");
                return;
            }
            sender.sendMessage(ChatColor.GREEN + "Whitelist is now on!");
            enforce_whitelist = true;

        } else if (args[0].equals("off")) {
            // disable the whitelist
            if (!enforce_whitelist) {
                sender.sendMessage(ChatColor.GREEN + "Whitelist is already off!");
                return;
            }
            sender.sendMessage(ChatColor.GREEN + "Whitelist is now off.");
            enforce_whitelist = false;

        } else {
            sender.sendMessage("Invalid arguments!");
        }
    }

    /**
     * @param list list of players to add
     * @return status message to player
     */
    private String add_list(String list) {
        if (active_lists.contains(list)) {
            return ChatColor.GREEN + "This list is already added.";
        }

        if (!lists.contains(list)) {
            return ChatColor.RED + "This list does not exist.";
        }

        active_lists.add(list);
        Bukkit.getLogger().info(list + " UUID list added.");
        return ChatColor.GREEN + list + " added to whitelist";
    }

    /**
     * @param list list of players to remove
     * @return status message to player
     */
    private String rem_list(String list) {
        if (list.equals("admin") || list.equals("temp")) {
            return ChatColor.RED + "This is a protected group & cannot be removed!";
        }

        if (!active_lists.contains(list) || list == null) {
            return ChatColor.RED + "This list does not exist or is inactive.";
        }

        active_lists.remove(list);
        Bukkit.getLogger().info(list + " UUID list removed.");
        return ChatColor.RED + list + " removed!";
    }

    /**
     * This player won't be able to log back in after the server restarts
     * @param temp_player
     */
    /**
     * This player won't be able to log back in after the server restarts
     * @param temp_player
     */
    ArrayList<PlayerID> temp_players = new ArrayList<>();
    ArrayList<String> temp_player_names = new ArrayList<>();
    private String add_temp(String temp_player_name) {
        try {
            // Create the connection
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + temp_player_name).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200) return "API Error: Player does not exist.";

            try (Reader reader = new InputStreamReader(connection.getInputStream())) {
                PlayerID data = new Gson().fromJson(reader, PlayerID.class);
                temp_players.add(data);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ChatColor.GREEN + "Added " + temp_player_name + " to temp whitelist.";
    }


    /**
     * Listen for login attempt and dispatch UUID checks
     * @param event
     */
    @EventHandler
    public void on_login(PlayerLoginEvent event) {
        if (!enforce_whitelist) {
            return;
        }

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (temp_player_names.contains(player.getName())) {
            Bukkit.getLogger().info(player.getName() + " is tempplayer.");
            return;
        }

        // check the rest of the lists
        for (String checking_list : active_lists) {
            // go through every list that is active at the moment
            try {
                Scanner searching_file = new Scanner(new File(this.plugin.getDataFolder(), checking_list + ".txt"));
                while (searching_file.hasNextLine()) {
                    String line = searching_file.nextLine();
                    if (line.equals(uuid.toString())) {
                        return;
                    }
                }
            } catch (FileNotFoundException e) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "java.io.FileNotFoundException@Whitelist.java: This is a bug!\n\n\nReport it to get a personal high five from heavy_fortress2.");
                Bukkit.getLogger().warning("Player " + event.getPlayer().getName() + " was kicked due to a FileNotFoundException");
                return;
            }
        }
        event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "Bigrat couldn't find you on the whitelist!\n\n\nContact an admin if you believe this to be an error!");
    }

    String[] base = {"admin"};
    List<String> lists = Arrays.asList(base);
    ArrayList<String> active_lists = new ArrayList<>();
    private void configure() {

        for (String group : lists) {
            File group_file = new File(this.plugin.getDataFolder(), group + ".txt");
//            if (group.equals("temp")) {
//                if (group_file.delete()) Bukkit.getLogger().info("Deleted temp whitelist.");
//            }
            try {
                if(group_file.createNewFile()) Bukkit.getLogger().info("Created new whitelist file: " + group_file.getName());
            } catch (IOException e) {
                Bukkit.getLogger().info("IOException: Failed to create new whitelist group file for group " + group_file.getName());
            }
        }
        active_lists.clear();
        active_lists.addAll(config_file.getStringList("whitelist.groups"));

        // the admin list is always added
        active_lists.add("admin");

        Bukkit.getLogger().info("active lists: " + String.join(", ", active_lists));
    }}