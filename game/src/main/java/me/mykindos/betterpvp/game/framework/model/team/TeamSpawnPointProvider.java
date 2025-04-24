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
import java.util.Random;

/**
 * Splits spawnpoints into groups
 */
public class TeamSpawnPointProvider implements SpawnPointProvider {

    private static final Random RANDOM = new Random();

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

        return spawnpoints.get(RANDOM.nextInt(spawnpoints.size())).getLocation();
    }
}
