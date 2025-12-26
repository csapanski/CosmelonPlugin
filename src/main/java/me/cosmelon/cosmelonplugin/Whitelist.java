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
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
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
        if (args == null || args.length == 0) {
            sender.sendMessage(invalid_args_msg);
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "add":
                // usage: /whitelist add <group>
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /whitelist add <group>");
                    return;
                }
                String group = args[1].toLowerCase();
                if (!lists.contains(group)) {
                    sender.sendMessage(ChatColor.RED + "That group does not exist");
                    return;
                }
                sender.sendMessage(add_list(group));
                break;

            case "remove":
                // usage: /whitelist remove <group>
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /whitelist remove <group>");
                    return;
                }
                sender.sendMessage(rem_list(args[1].toLowerCase()));
                break;

            case "list": {
                // /whitelist list -> show active and  available groups
                // /whitelist list <group> -> show UUIDs in group file
                if (args.length == 1) {
                    String s = active_lists.size() + " group(s) active: ";
                    for (String name : active_lists) {
                        s += name + ", ";
                    }
                    sender.sendMessage(s);

                    s = lists.size() + " group(s) available: ";
                    for (String name : lists) {
                        s += name + ", ";
                    }
                    sender.sendMessage(s);
                    return;
                } else {
                    final String gr = args[1].toLowerCase();
                    if (!lists.contains(gr)) {
                        sender.sendMessage(ChatColor.RED + "That group does not exist.");
                        return;
                    }
                    File file = new File(this.plugin.getDataFolder(), gr + ".txt");
                    if (!file.exists()) {
                        sender.sendMessage(ChatColor.RED + "No file for group " + gr + " found.");
                        return;
                    }
                    String str = "Contents of " + gr + ": ";
                    try (Scanner scanner = new Scanner(file)) {
                        boolean any = false;
                        while (scanner.hasNextLine()) {
                            String line = scanner.nextLine().trim();
                            if (line.isEmpty()) continue;
                            if (any) str += ", ";
                            str += line;
                            any = true;
                        }
                        if (!any) str += "[empty]";
                        sender.sendMessage(str);
                    } catch (FileNotFoundException e) {
                        sender.sendMessage(ChatColor.RED + "Failed to read group file: " + e.getMessage());
                    }
                    return;
                }
            }

            case "tempplayer":
                // /whitelist tempplayer -list
                // /whitelist tempplayer <name>
                if (args.length != 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /whitelist tempplayer <name| -list>");
                    return;
                }
                String param = args[1];
                if (param.equalsIgnoreCase("-list")) {
                    if (temp_player_names.isEmpty()) {
                        sender.sendMessage("Temp players: (none)");
                        return;
                    }
                    String s = "Temp players: ";
                    for (String player_name : temp_player_names) {
                        s += player_name + ", ";
                    }
                    sender.sendMessage(s);
                    return;
                }

                String name = param.toLowerCase(); // normalize to lowercase
                if (temp_player_names.contains(name)) {
                    temp_player_names.remove(name);
                    sender.sendMessage(ChatColor.GREEN + param + " removed from temp whitelist.");
                } else {
                    temp_player_names.add(name);
                    sender.sendMessage(ChatColor.GREEN + param + " added to temp whitelist.");
                }
                break;

            case "on":
                if (enforce_whitelist) {
                    sender.sendMessage(ChatColor.RED + "Whitelist is already on");
                } else {
                    enforce_whitelist = true;
                    sender.sendMessage(ChatColor.GREEN + "Whitelist is now on");
                }
                break;

            case "off":
                if (!enforce_whitelist) {
                    sender.sendMessage(ChatColor.RED + "Whitelist is already off");
                } else {
                    enforce_whitelist = false;
                    sender.sendMessage(ChatColor.GREEN + "Whitelist is now off");
                }
                break;

            case "help":
                sender.sendMessage(ChatColor.AQUA + "Whitelist commands:");
                sender.sendMessage("/whitelist list");
                sender.sendMessage("/whitelist list <group>");
                sender.sendMessage("/whitelist add <group>");
                sender.sendMessage("/whitelist remove <group>");
                sender.sendMessage("/whitelist tempplayer <name>");
                sender.sendMessage("/whitelist on|off");
                break;

            default:
                sender.sendMessage(invalid_args_msg);
                return;
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
        if (list.equals("admin")) {
            return ChatColor.RED + "admin is a protected group & cannot be removed!";
        }

        if (list == null || !active_lists.contains(list)) {
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
//    ArrayList<PlayerID> temp_players = new ArrayList<>();
    ArrayList<String> temp_player_names = new ArrayList<>();
//    private String add_temp(String temp_player_name) {
//        try {
//            // Create the connection
//            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + temp_player_name).openConnection();
//            connection.setRequestMethod("GET");
//            connection.setRequestProperty("Accept", "application/json");
//
//            if (connection.getResponseCode() != 200) return "API Error: Player does not exist.";
//
//            try (Reader reader = new InputStreamReader(connection.getInputStream())) {
//                PlayerID data = new Gson().fromJson(reader, PlayerID.class);
//                temp_players.add(data);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return ChatColor.GREEN + "Added " + temp_player_name + " to temp whitelist.";
//    }


    /**
     * Listen for login attempt and dispatch UUID checks
     * @param event
     */
    @EventHandler
    public void on_login(AsyncPlayerPreLoginEvent event) {
        if (!enforce_whitelist) {
            return;
        }

        final String playerName = event.getName();
        UUID uuid = event.getUniqueId();

        if (temp_player_names.contains(playerName.toLowerCase())) {
            Bukkit.getLogger().info(playerName + " is tempplayer.");
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
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "java.io.FileNotFoundException@Whitelist.java: This is a bug!\n\n\nReport it to get a personal high five from heavy_fortress2.");
                Bukkit.getLogger().warning("Player " + playerName + " was kicked due to a FileNotFoundException");
                return;
            }
        }
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, "Bigrat couldn't find you on the whitelist!\n\n\nContact an admin if you believe this to be an error!");
    }

    List<String> lists = new ArrayList<>();
    ArrayList<String> active_lists = new ArrayList<>();

    private void configure() {
        lists.clear();
        lists.addAll(config_file.getStringList("whitelist.groups"));

        if (!lists.contains("admin")) lists.add("admin");

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
        // the admin list is always added
        active_lists.add("admin");

        Bukkit.getLogger().info("active lists: " + String.join(", ", active_lists));
    }}