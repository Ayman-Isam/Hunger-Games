package me.cantankerousally.hungergames.handler;

import me.cantankerousally.hungergames.HungerGames;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class SetSpawnHandler implements Listener {

    private HungerGames plugin;

    public SetSpawnHandler(HungerGames plugin) {
        this.plugin = plugin;
    }

    private Set<String> occupiedSpawnPoints = new HashSet<>();
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType() == Material.STICK && item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.getDisplayName().equals(ChatColor.AQUA + "Spawn Point Selector")) {
                if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    Location location = event.getClickedBlock().getLocation();
                    // Save location to config.yml
                    List<String> spawnPoints = plugin.getConfig().getStringList("spawnpoints");
                    if (spawnPoints.size() < 24) {
                        spawnPoints.add(location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ());
                        plugin.getConfig().set("spawnpoints", spawnPoints);
                        plugin.saveConfig();
                        player.sendMessage(ChatColor.GREEN + "Spawn point set at X: " + location.getBlockX() + " Y: " + location.getBlockY() + " Z: " + location.getBlockZ());
                    } else if (spawnPoints.size() ==  24){
                        player.sendMessage(ChatColor.RED + "You have reached the maximum number of spawn points!");
                    }
                    event.setCancelled(true);
                }
            }
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block.getType() == Material.BAMBOO_HANGING_SIGN || block.getType() == Material.OAK_WALL_SIGN) {
                Sign sign = (Sign) block.getState();
                if (sign.getLine(0).equalsIgnoreCase("[Join]")) {
                    if (plugin.gameStarted) {
                        player.sendMessage(ChatColor.RED + "The game has already started!");
                        return;
                    }
                    // Teleport player to an unoccupied spawn point
                    List<String> spawnPoints = plugin.getConfig().getStringList("spawnpoints");
                    List<String> availableSpawnPoints = new ArrayList<>(spawnPoints);
                    availableSpawnPoints.removeAll(occupiedSpawnPoints);
                    if (!availableSpawnPoints.isEmpty()) {
                        String spawnPoint = availableSpawnPoints.get(new Random().nextInt(availableSpawnPoints.size()));
                        String[] coords = spawnPoint.split(",");
                        World world = plugin.getServer().getWorld(coords[0]);
                        double x = Double.parseDouble(coords[1]);
                        double y = Double.parseDouble(coords[2]) + 1;
                        double z = Double.parseDouble(coords[3]);
                        player.teleport(new Location(world, x, y, z));
                        occupiedSpawnPoints.add(spawnPoint);
                        player.setGameMode(GameMode.ADVENTURE);
                        player.getInventory().clear();
                        player.setExp(0);
                        player.setLevel(30);
                        player.setHealth(20);
                        player.setFoodLevel(20);
                        for (PotionEffect effect : player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        // Broadcast a message to all players
                        plugin.getServer().broadcastMessage(ChatColor.AQUA + player.getName() + " has joined [" + occupiedSpawnPoints.size() + "/" + spawnPoints.size() + "]");

                    } else {
                        player.sendMessage(ChatColor.RED + "All spawn points are currently occupied!");
                    }
                }
            }
        }
    }

    public void clearOccupiedSpawnPoints() {
        occupiedSpawnPoints.clear();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (block.getType() == Material.BAMBOO_HANGING_SIGN || block.getType() == Material.OAK_WALL_SIGN) {
            Sign sign = (Sign) block.getState();
            if (sign.getLine(0).equalsIgnoreCase("[Join]")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean gameStarted = false;

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
            // Retrieve the coordinates from the config.yml file
            double x1 = plugin.getConfig().getDouble("region.pos1.x");
            double y1 = plugin.getConfig().getDouble("region.pos1.y");
            double z1 = plugin.getConfig().getDouble("region.pos1.z");
            double x2 = plugin.getConfig().getDouble("region.pos2.x");
            double y2 = plugin.getConfig().getDouble("region.pos2.y");
            double z2 = plugin.getConfig().getDouble("region.pos2.z");

            // Check if the player is within the specified coordinates and the game has not started
            if (!plugin.gameStarted && to.getX() >= Math.min(x1, x2) && to.getX() <= Math.max(x1, x2) && to.getY() >= Math.min(y1, y2) && to.getY() <= Math.max(y1, y2) && to.getZ() >= Math.min(z1, z2) && to.getZ() <= Math.max(z1, z2)) {
                // Cancel the event to prevent the player from moving
                if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                    // Cancel the event to prevent non-creative and non-spectator players from moving
                    event.setCancelled(true);
                }
            }
        }
    }
    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        if (event.getBlock().getType() == Material.ANVIL || event.getBlock().getType() == Material.CHIPPED_ANVIL || event.getBlock().getType() == Material.DAMAGED_ANVIL) {
            event.setCancelled(true);
        }
    }
}