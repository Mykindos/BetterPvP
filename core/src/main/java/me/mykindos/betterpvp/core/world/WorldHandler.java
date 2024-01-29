package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.HashMap;
import java.util.Objects;

@Singleton
@Slf4j
public class WorldHandler {

    private final Core core;

    @Getter
    private final HashMap<String, Location> spawnLocations;

    @Inject
    public WorldHandler(Core core) {
        this.core = core;
        this.spawnLocations = new HashMap<>();
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
}
