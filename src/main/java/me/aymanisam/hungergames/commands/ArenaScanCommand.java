package me.aymanisam.hungergames.commands;

import me.aymanisam.hungergames.HungerGames;
import me.aymanisam.hungergames.handlers.ArenaHandler;
import me.aymanisam.hungergames.handlers.LangHandler;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ArenaScanCommand implements CommandExecutor {
    private final HungerGames plugin;
    private final LangHandler langHandler;
    private final ArenaHandler arenaHandler;

    public ArenaScanCommand(HungerGames plugin) {
        this.plugin = plugin;
        this.langHandler = new LangHandler(plugin);
        this.arenaHandler = new ArenaHandler(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(langHandler.getMessage("no-server"));
            return true;
        }

        langHandler.getLangConfig(player);

        if (!(player.hasPermission("hungergames.scanarena"))) {
            sender.sendMessage(langHandler.getMessage("no-permission"));
            return true;
        }

        FileConfiguration config = arenaHandler.getArenaConfig();

        if (!config.isSet("region.pos1.x") || !config.isSet("region.pos1.y") || !config.isSet("region.pos1.z")
                || !config.isSet("region.pos2.x") || !config.isSet("region.pos2.y") || !config.isSet("region.pos2.z")) {
            sender.sendMessage(langHandler.getMessage("scanarena.region-undef"));
            return true;
        }

        World world = plugin.getServer().getWorld(Objects.requireNonNull(config.getString("region.world")));
        double pos1x = config.getDouble("region.pos1.x");
        double pos1z = config.getDouble("region.pos1.z");
        double pos2x = config.getDouble("region.pos2.x");
        double pos2z = config.getDouble("region.pos2.z");

        int minX = (int) Math.min(pos1x, pos2x);
        int minZ = (int) Math.min(pos1z, pos2z);
        int maxX = (int) Math.max(pos1x, pos2x);
        int maxZ = (int) Math.max(pos1z, pos2z);

        File chestLocationsFile = new File(plugin.getDataFolder(), "chest-locations.yml");

        List<Location> chestLocations = new ArrayList<>();
        List<Location> barrelLocations = new ArrayList<>();
        List<Location> trappedChestLocations = new ArrayList<>();

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {

                assert world != null;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ);
                }
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                for (BlockState blockState : chunk.getTileEntities()) {
                    if (blockState instanceof Chest) {
                        Material type = blockState.getType();
                        if (type == Material.CHEST) {
                            chestLocations.add(blockState.getLocation());
                        } else if (type == Material.TRAPPED_CHEST) {
                            trappedChestLocations.add(blockState.getLocation());
                        }
                    } else if (blockState instanceof Barrel) {
                        barrelLocations.add(blockState.getLocation());
                    }
                }
            }
        }

        FileConfiguration chestLocationsConfig = new YamlConfiguration();
        chestLocationsConfig.set("chest-locations", chestLocations.stream()
                .map(Location::serialize)
                .collect(Collectors.toList()));
        chestLocationsConfig.set("barrel-locations", barrelLocations.stream()
                .map(Location::serialize)
                .collect(Collectors.toList()));
        chestLocationsConfig.set("trapped-chests-locations", trappedChestLocations.stream()
                .map(Location::serialize)
                .collect(Collectors.toList()));
        try {
            chestLocationsConfig.save(chestLocationsFile);
            sender.sendMessage(langHandler.getMessage("scanarena.saved-locations"));
        } catch (IOException e) {
            sender.sendMessage(langHandler.getMessage("scanarena.failed-locations"));
        }

        player.sendMessage(langHandler.getMessage("scanarena.found-chests") + chestLocations.size());
        player.sendMessage(langHandler.getMessage("scanarena.found-barrels") + barrelLocations.size());
        player.sendMessage(langHandler.getMessage("scanarena.found-trapped-chests") + trappedChestLocations.size());

        return true;
    }
}
