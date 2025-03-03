package me.mykindos.betterpvp.game.framework.configuration;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * A generic configuration for a game, which can be extended by specific game types for
 * additional configuration.
 */
@Getter
@SuperBuilder
public class GenericGameConfiguration {

    private final @NotNull String name;
    @Builder.Default
    private final @NotNull Duration duration = Duration.ofMinutes(10);
    private final int requiredPlayers;

    public void validate() {
        Preconditions.checkArgument(!name.isEmpty(), "name cannot be empty");
        Preconditions.checkArgument(duration.toMinutes() > 0, "duration minutes must be greater than 0");
        Preconditions.checkArgument(requiredPlayers > 0, "requiredPlayers must be greater than 0");
    }

}
