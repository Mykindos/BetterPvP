package me.mykindos.betterpvp.game.framework.configuration;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;
import me.mykindos.betterpvp.game.framework.model.team.TeamProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A configuration for a team-based game
 */
@Getter
@SuperBuilder
public class TeamGameConfiguration extends GenericGameConfiguration {

    @Singular
    private final @NotNull Set<@NotNull TeamProperties> teamProperties;

    @Override
    public void validate() {
        Preconditions.checkArgument(teamProperties.size() >= 2, "Must have at least 2 teams");
        super.validate();
    }

}
