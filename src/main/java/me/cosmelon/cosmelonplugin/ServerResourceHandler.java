package me.cosmelon.cosmelonplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.cosmelon.cosmelonplugin.misc.PastelDay;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * This class handles parse/loading config data and http ports for the resource pack host
 */
public class ServerResourceHandler {

    private FileConfiguration config_file;
    private CosmelonPlugin plugin;

    private PastelDay pd;
    ServerResourceHandler(CosmelonPlugin plugin) {
        this.plugin = plugin;
        this.config_file = plugin.manager.getConfig();

        loadFromConfig();
        start(resourcePacks);

        this.pd = new PastelDay(this, plugin);
    }

    /**
     * Send the proper resource pack to a player.
      * @param player
     */
    public void applyresources(Player player) {
        player.removeResourcePacks();

        // pastel pack
        if (!plugin.bigratStatus() && pd.getPastelStatus()) {
            player.addResourcePack(UUID.randomUUID(), getUrl("pastel-day"), null, "Installing Billy day pack...", true);
        }

        // mainpack
        player.addResourcePack(UUID.randomUUID(), getUrl("mainpack"), null, "Installing mainpack", true);

        // accessibility extensions
        if (player.getScoreboardTags().contains("br_colorblind")) {
            player.addResourcePack(UUID.randomUUID(), getUrl("colorblind"), null, "Install accessibility pack?", true);
        }
    }

    /**
     * Parse config.yml for resource packs.
     */
    private String ip;
    private ArrayList<ServerResourcePack> resourcePacks;
    // load from config
    private void loadFromConfig() {
        // grab packs from config
        resourcePacks = new ArrayList<>();
        ArrayList<String> pack_names = new ArrayList<>(config_file.getConfigurationSection("pack-list").getKeys(false));
        Bukkit.getConsoleSender().sendMessage(pack_names.size() + " pack(s) loading from config:");

        for (String pack_name : pack_names) {
            // create pack object
            ServerResourcePack pack = new ServerResourcePack(pack_name,
                config_file.getInt("pack-list." + pack_name + ".port"),
                config_file.getString("pack-list." + pack_name + ".location"));

            resourcePacks.add(pack);
            Bukkit.getConsoleSender().sendMessage(pack.toString());
        }

    }

    /**
     * Take an ArrayList of ServerResourcePack and start http servers for each of them
     * @param packList
     */
    ArrayList<WebHandler> server_list;
    private void start(ArrayList<ServerResourcePack> packList) {
        server_list = new ArrayList<>();
        nameurl = new HashMap();
        for (ServerResourcePack current : packList) {
            WebHandler temp = new WebHandler(current.getName(), current.getPort(), current.getLocation(), this.plugin);
            server_list.add(temp);

            // add to lookup map
            nameurl.put(temp.getName(), temp.getUrl());
        }
    }


    /**
     * Stop all http servers
     */
    protected void stop() {
        if (server_list != null) {
            for (WebHandler webHandler : this.server_list) {
                webHandler.stop();
            }
        }
    }

    /**
     * @param - String packname as specified in config.yml
     * @returns - url for specified packname
     */
    Map<String, String> nameurl;
    protected String getUrl(String packname) {
        return nameurl.get(packname);
    }

    protected UUID getUUID(String packname) {
        for (ServerResourcePack current : resourcePacks) {
            if (current.getName().equals(packname)) {
                return current.getUUID();
            }
        }
        return null;
    }

}
