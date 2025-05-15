package me.mykindos.betterpvp.game.framework.model.team;

import com.google.common.base.Preconditions;
import dev.brauw.mapper.region.PerspectiveRegion;
import me.mykindos.betterpvp.game.framework.AbstractGame;
import me.mykindos.betterpvp.game.framework.TeamGame;
import me.mykindos.betterpvp.game.framework.model.spawnpoint.SpawnPointProvider;
import me.mykindos.betterpvp.game.framework.model.world.MappedWorld;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Splits spawnpoints into groups
 */
public class TeamSpawnPointProvider implements SpawnPointProvider {
    private final Map<Team, Integer> teamLastSpawn = new ConcurrentHashMap<>(2);

    @Override
    public @NotNull Location getSpawnPoint(Player player, MappedWorld map, AbstractGame<?, ?> game) {
        Preconditions.checkArgument(game instanceof TeamGame, "Game is not a team game");
        TeamGame<?> teamGame = (TeamGame<?>) game;
        final Team team = teamGame.getPlayerTeam(player);
        Preconditions.checkState(team != null, "Player is not in a team");

        List<PerspectiveRegion> spawnpoints = map
                .findRegion("spawnpoint_" + team.getProperties().name().toLowerCase(), PerspectiveRegion.class)
                .toList();

        if (spawnpoints.isEmpty()) {
            throw new IllegalStateException("No spawnpoints found for team " + team.getProperties().name());
        }

        //Sequentially supply spawnpoints to reduce multiple players spawning on the same spawnpoint
        int nextSpawn = teamLastSpawn.getOrDefault(team, -1) + 1;
        if (nextSpawn >= spawnpoints.size()) {
            nextSpawn = 0;
        }
        teamLastSpawn.put(team, nextSpawn);

        return spawnpoints.get(nextSpawn).getLocation();
    }
}
