package me.cosmelon.cosmelonplugin;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Web server to issue resource pack files to client
 */

public class WebHandler {
    private Logger log;
    private HttpServer httpServer;
    String packname;
    String ip;
    int port;
    String packlocation;
    public boolean online;

    /**
     * constructor
     * @param packname
     * @param port
     * @param packlocation
     * @param plugin - parent plugin
     */
    private CosmelonPlugin plugin;
    WebHandler(String packname, int port, String packlocation, CosmelonPlugin plugin) {
        this.packname = packname;
        this.port = port;
        this.packlocation = packlocation;
        this.plugin = plugin;
        this.log = Bukkit.getLogger();
        start();
    }

    public boolean start() {
        ip = plugin.getServer().getIp();
        if (ip == null || ip.equals("")) ip = "localhost";

        try {
            stop(); // ensure previous instance is stopped
            httpServer = Vertx.vertx().createHttpServer();
            httpServer.requestHandler(httpServerRequest -> httpServerRequest.response().sendFile(getFileLocation()));
            httpServer.listen(port);
            log.info(this.packname + " is now live at http://" + this.ip + ":" + this.port);
            this.online = true;
        } catch (Exception e) {
            log.warning("Unable to bind to port for '" + packname + "', select a different port!");
            this.online = false;
            return false;
        }
        return true;
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.close();
            this.online = false;
        }
    }

    private String getFileLocation() {
        // Construct the file path relative to the plugin's data folder
        String filePath = plugin.getDataFolder().getPath() + File.separator + packlocation;
        File file = new File(filePath);
        String absolutePath = file.getAbsolutePath();
        if (!file.exists()) {
            log.warning("File not found at location: " + absolutePath);
            return null; // Return null if the file doesn't exist
        }
        return absolutePath;
    }
    
    protected String getName() {
        return this.packname;
    }

    protected String getIp() {
        return this.ip;
    }

    protected int getPort() {
        return this.port;
    }

    protected String getUrl() {
        // existing code
        // Player player = Bukkit.getPlayer("Cosmelon");
        // player.sendMessage("getURL() method:   http://" + this.ip + ":" + this.port + this.getFileLocation());
        // Instead of sending a message to a specific player, you can log it or handle it differently
//        Bukkit.getPlayer("Cosmelon").sendMessage(ChatColor.GREEN + "URL: http://" + this.ip + ":" + this.port + this.getFileLocation());
        return "http://" + this.ip + ":" + this.port + this.getFileLocation();
    }
}
