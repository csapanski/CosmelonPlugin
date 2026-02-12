package me.cosmelon.cosmelonplugin.listeners;

import static java.lang.Math.random;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import me.cosmelon.cosmelonplugin.CosmelonPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ElytraListener implements Listener {

    CosmelonPlugin plugin;

    // terminal velocity
    boolean max_vel;
    double maxvel_mps;
    double maxvel_mpt;
    int elytra_dry_duration;

    public ElytraListener(CosmelonPlugin plugin) {
        this.plugin = plugin;

        // disable for bigrat
        if (plugin.getConfig().getBoolean("bigrat")) return;

        this.max_vel = plugin.getConfig().getBoolean("elytra-nerf.terminal-velocity-enabled");
        // Convert from m/s to m/tick
        this.maxvel_mps = plugin.getConfig().getDouble("elytra-nerf.terminal-velocity-speed");
        this.maxvel_mpt = maxvel_mps/20.0;


        this.nether_nerf = plugin.getConfig().getBoolean("elytra-nerf.require-gold-in-nether");
        this.overworld_nerf = plugin.getConfig().getBoolean("elytra-nerf.overworld-nerf");
        this.overworld_sick_ydiff = plugin.getConfig().getInt("elytra-nerf.overworld-sick-ydiff");
        this.elytra_dry_duration = plugin.getConfig().getInt("elytra-nerf.elytra-dry-duration", 20);



        Bukkit.getPluginManager().registerEvents(this, plugin);

        // actionbar for wet elytra
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            glide_cooldown.entrySet().removeIf(entry -> {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) return true;
                Long remaining = entry.getValue() - now;

                if (remaining <= 0) {
                    player.sendActionBar(Component.empty());
                    return true;
                }

                int percent = (int) ((remaining / ((double) elytra_dry_duration * 1000)) * 100);
                player.sendActionBar(Component.text("Wings Drying: " + percent + "%").color(NamedTextColor.GOLD));
                return false;
            });
        }, 0L, 0L);

    }


    boolean nether_nerf;
    boolean overworld_nerf;
    int overworld_sick_ydiff;
    @EventHandler
    public void ElytraFly(PlayerMoveEvent event) {
        Player p = event.getPlayer();
        boolean gliding = p.isGliding();
        boolean wearing_elytra = p.getInventory().getChestplate() != null
                && p.getInventory().getChestplate().getType() == Material.ELYTRA;
        final World.Environment dimension = p.getWorld().getEnvironment();

        // early exit
        if (event.getFrom().getBlockY() == event.getTo().getBlockY()
            || p.getGameMode() != GameMode.SURVIVAL
            || !wearing_elytra) return;

        // need >1 piece of gold armor
        final boolean gold_armor = check_gold_armor(p.getInventory().getArmorContents());

        // prevent gliding in liquid... feat. dumb hack?
        Location liq_loc = p.getLocation();
        liq_loc.setY(liq_loc.getBlockY()-1);
        if (p.isInWater() || liq_loc.getBlock().equals(Material.WATER)
            || p.isInLava() || liq_loc.getBlock().equals(Material.LAVA)) {
            inLiquid_nerf(p);
            return;
        }

        if (p.isInRain()) {
            glide_cooldown.put(p.getUniqueId(), System.currentTimeMillis() + elytra_dry_duration*INTERVAL_MS);
        }

        // standard ruleset
        if (gliding) {
            final Location loc = p.getLocation();

            // require leather armor for flying high in overworld
            if (dimension == World.Environment.NORMAL && overworld_nerf) {
                // do this rule based on distance from the ground
                int dist = dist_from_ground(loc);
                if (dist > overworld_sick_ydiff) {
                    altitude_sickness(p, dist);
                } else {
                    in_clouds.remove(p.getUniqueId());
                }
            }

            // require gold armor for flying in the nether
            else if (dimension == World.Environment.NETHER && nether_nerf) {
                // disable above nether roof
                if (p.getLocation().getBlockY() > 190) {
                    p.sendActionBar(Component.text("Elytra is disabled above the roof, working on a better balance --Cos")
                        .color(NamedTextColor.YELLOW));
                    p.setGliding(false);
                }
                // gold armor feature
                else if (!gold_armor) {
                    fire_damage(p);
                }

                // speed cap
                if (p.getVelocity().length() > maxvel_mpt) {
                    p.setVelocity(p.getVelocity().normalize().multiply(maxvel_mpt));
                }
            }
        }

        // Nether warning when equipped elytra
        if (dimension == World.Environment.NETHER && !gold_armor) {
            p.sendTitle("", ChatColor.GREEN + "Golden Armor required to Fly in Nether!", 0, 40, 10);
        }

    }

    // prevent memory leak
    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        last_altitude_tick.remove(uuid);
        in_clouds.remove(uuid);
        glide_cooldown.remove(uuid);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        last_altitude_tick.remove(uuid);
        in_clouds.remove(uuid);
        glide_cooldown.remove(uuid);
    }


    // glide cooldown
    private final Map<UUID, Long> glide_cooldown = new HashMap<>();
    @EventHandler
    public void elytra_cooldown(EntityToggleGlideEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (!glide_cooldown.containsKey(player.getUniqueId())) return;
        if (player.isInRain() && player.isGliding()) return;

        Location liq_loc = player.getLocation();
        liq_loc.setY(liq_loc.getBlockY()-1);

        if (e.isGliding()) {
            e.setCancelled(true);
            player.sendMessage(ChatColor.RED + "» Your Elytra is too wet to fly!");
        }
    }

    /**
     * disable firework boosting while flying thru rain
     * @param e
     */
    @EventHandler
    public void rain_check(PlayerElytraBoostEvent e) {
        if (e.getPlayer() == null) return;
        Player p = e.getPlayer();

        // check if it is raining
        if (p.isInRain()) {
            e.setCancelled(true);
            e.setShouldConsume(false);
            p.showTitle(rain_fly_title);
        }
    }

    final Title rain_fly_title = Title.title(
        Component.text("Land safely!").color(NamedTextColor.RED),
        Component.text("Wings are too wet to boost!").color(NamedTextColor.YELLOW),
        Title.Times.times(
            Duration.ofMillis(0),
            Duration.ofSeconds(3),
            Duration.ofMillis(300)
        )
    );

    private final Map<UUID, Long> last_altitude_tick = new HashMap<>();
    private final HashSet<UUID> in_clouds = new HashSet<>();

    private final long INTERVAL_MS = 1000;

    private void altitude_sickness(final Player player, final int dist) {
        final UUID uuid = player.getUniqueId();
        int y_val = player.getLocation().getBlockY();

        long now = System.currentTimeMillis();
        long last = last_altitude_tick.getOrDefault(uuid, 0L);

        if (now - last < INTERVAL_MS) return;
        last_altitude_tick.put(uuid, now);

        // send the title once per clouds
        if (!in_clouds.contains(uuid)) {
            player.showTitle(dizzy_title);
            in_clouds.add(uuid);
        }

        // hunger with drag down

        if (dist > (overworld_sick_ydiff * 2)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 1));

            int effect = (int) (random() * 10) % 2;
            if (effect == 0) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 40, 0));
            } else if (effect == 1) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0));
            }
        } else {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0));
        }
    }

    final Component altmsg = Component.text("You're starting to feel dizzy...", NamedTextColor.DARK_GREEN)
        .decorate(TextDecoration.ITALIC);

    final Title dizzy_title = Title.title(
        Component.text("Altitude Sickness!").color(NamedTextColor.GREEN),
        altmsg,
        Title.Times.times(
            Duration.ofMillis(0),
            Duration.ofSeconds(3),
            Duration.ofMillis(0)
        )
    );

    private void inLiquid_nerf(Player player) {
        // assume the player is wearing elytra already
        glide_cooldown.put(player.getUniqueId(), System.currentTimeMillis() + elytra_dry_duration*INTERVAL_MS);
        player.showTitle(wet_title);
        player.setGliding(false);
    }

    final Title wet_title = Title.title(Component.text(""),
        Component.text("Damp Wings!").color(NamedTextColor.RED),
        Title.Times.times(
            Duration.ofMillis(0),
            Duration.ofSeconds(3),
            Duration.ofMillis(0)
        )
    );


    // this is a "special" kind of fire damage that ignores fire resistance
    private final Map<UUID, Long> last_fire_tick = new HashMap<>();
    private void fire_damage(Player player) {
        long now = System.currentTimeMillis();
        long last = last_fire_tick.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < INTERVAL_MS) return;
        last_fire_tick.put(player.getUniqueId(), now);

        // I'm not too evil now come on!
        if (player.getHealth() < 13) return;

        // ignore fire resistance but provide a fallback
        if (player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE)) {
            player.damage(1);
            player.sendActionBar(Component.text("Fire resistance has no effect! Land soon!")
                .color(NamedTextColor.GREEN));
        } else {
            player.setFireTicks(21);
        }
        player.showTitle(fire_title);
    }

    final Component firemsg = Component.text("You're starting to burn up...", NamedTextColor.DARK_RED)
        .decorate(TextDecoration.ITALIC);

    final Title fire_title = Title.title(
        Component.text(""),
        firemsg,
        Title.Times.times(
            Duration.ofMillis(200),
            Duration.ofSeconds(3),
            Duration.ofMillis(200)
        )
    );

    private int dist_from_ground(Location loc) {
        World world = loc.getWorld();
        int px = loc.getBlockX();
        int pz = loc.getBlockZ();
        int py = loc.getBlockY();

        int radius = plugin.getConfig().getInt("elytra-nerf.ground-sample-radius", 2);

        int weightedSum = 0;
        int weightTotal = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq > radius * radius) continue;
                int groundY = world.getHighestBlockYAt(px + dx, pz + dz);
                // closer samples matter more
                int weight = (radius * radius - distSq) + 1;
                weightedSum += groundY * weight;
                weightTotal += weight;
            }
        }

        int avgGroundY = weightedSum / weightTotal;
        return py - avgGroundY;
    }



    // as it was done, it is said
    private boolean check_gold_armor(ItemStack[] stacks) {
        for (ItemStack piece : stacks) {
            if (piece == null) continue;

            Material type = piece.getType();
            if (type == Material.GOLDEN_BOOTS
                || type == Material.GOLDEN_LEGGINGS
                || type == Material.GOLDEN_CHESTPLATE
                || type == Material.GOLDEN_HELMET) {
                return true;
            }
        }
        return false;
    }


    private boolean check_leather_armor(ItemStack[] stacks) {
        for (ItemStack piece : stacks) {
            if (piece == null) continue;

            Material type = piece.getType();
            if (type == Material.LEATHER_BOOTS
                && type == Material.LEATHER_LEGGINGS
                && type == Material.LEATHER_HELMET) {
                return true;
            }
        }
        return false;
    }

}
