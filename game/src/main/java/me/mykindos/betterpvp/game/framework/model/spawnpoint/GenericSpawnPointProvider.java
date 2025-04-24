package me.mykindos.betterpvp.game.framework.model.spawnpoint;

import dev.brauw.mapper.region.PerspectiveRegion;
import lombok.CustomLog;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

/**
 * Gets a random spawnpoint from the map. Any region called "spawnpoint" will be used.
 */
@CustomLog
public class GenericSpawnPointProvider implements SpawnPointProvider {

    private static final Random RANDOM = new Random();

    @Override
    public @NotNull Location getSpawnPoint(Player player, MappedWorld map, AbstractGame<?, ?> game) {
        List<PerspectiveRegion> spawnpoints = map.findRegion("spawnpoint", PerspectiveRegion.class).toList();
        if (spawnpoints.isEmpty()) {
            throw new IllegalStateException("No spawnpoints found in map");
        }

        return spawnpoints.get(RANDOM.nextInt(spawnpoints.size())).getLocation();
    }
}
