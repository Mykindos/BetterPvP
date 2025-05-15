package me.mykindos.betterpvp.game.framework.model.attribute.bound;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Attribute for configuring the game duration.
 */
@GameScoped
public class GameDurationAttribute extends BoundAttribute<Duration> {

    @Inject
    public GameDurationAttribute() {
        super("game.duration", Duration.ofMinutes(10));
    }

    @Override
    public boolean isValidValue(Duration value) {
        // Must be positive
        return !value.isNegative() && !value.isZero();
    }

    @Override
    @Nullable
    public Duration parseValue(String value) {
        try {
            // Parse as minutes
            long minutes = Long.parseLong(value);
            return Duration.ofMinutes(minutes);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public String formatValue(@NotNull Duration value) {
        return value.toMinutes() + " minutes";
    }

    @Override
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Game duration must be a positive number of minutes.";
    }

}
