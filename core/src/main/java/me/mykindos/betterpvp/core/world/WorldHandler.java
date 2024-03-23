package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@CustomLog
public class WorldHandler {

    private final Core core;

    @Getter
    private final HashMap<String, Location> spawnLocations;

    @Inject
    public WorldHandler(Core core) {
        this.core = core;
        this.spawnLocations = new HashMap<>();
    }

    public Set<BPvPWorld> getWorlds() {
        final Set<BPvPWorld> worlds = new HashSet<>();
        worlds.addAll(Bukkit.getWorlds().stream().map(BPvPWorld::new).toList());
        worlds.addAll(UtilWorld.getUnloadedWorlds().stream().map(BPvPWorld::new).toList());

        return worlds.stream()
                .sorted(Comparator.comparing(BPvPWorld::isLoaded).reversed()
                        .thenComparing(BPvPWorld::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Location getSpawnLocation() {
        // return random location from spawnLocations, assuming it has values
        if (!spawnLocations.isEmpty()) {
            return spawnLocations.values().stream().toList().get(UtilMath.randomInt(spawnLocations.size()));
        } else {
            // return default spawn location
            return Objects.requireNonNull(Bukkit.getWorld("world")).getSpawnLocation();
        }
    }

    public void loadSpawnLocations() {
        spawnLocations.clear();

        var configSection = core.getConfig().getConfigurationSection("spawns");
        if(configSection == null) return;

        configSection.getKeys(false).forEach(key -> {
            Location spawnPoint = UtilWorld.stringToLocation(configSection.getString(key));
            spawnLocations.put(key, spawnPoint);
            log.info("Loaded spawn point {} at {}", key, spawnPoint);
        });
    }

    @SneakyThrows
    public void deleteWorld(BPvPWorld world) {
        world.unloadWorld();
        if (world.getWorldFolder().exists()) {
            FileUtils.forceDelete(world.getWorldFolder());
        }
    }
}
