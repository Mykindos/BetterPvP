package me.mykindos.betterpvp.game.framework;

import me.mykindos.betterpvp.game.framework.configuration.TeamGameConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a team game with an assigned {@link TeamGameConfiguration}
 */
public non-sealed class TeamGame extends AbstractGame<TeamGameConfiguration> {

    protected TeamGame(@NotNull TeamGameConfiguration configuration) {
        super(configuration);
    }
}
