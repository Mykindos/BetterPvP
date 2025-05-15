package me.mykindos.betterpvp.game.framework.model.attribute.team;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Attribute for determining how long a team is unbalanced before it should autobalance
 */
@GameScoped
public class DurationBeforeAutoBalanceAttribute extends BoundAttribute<Duration> {

        @Inject
        public DurationBeforeAutoBalanceAttribute() {
            super("game.auto-balance-duration", Duration.ofSeconds(10));
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
                // Parse as seconds
                long seconds = Long.parseLong(value);
                return Duration.ofSeconds(seconds);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        @NotNull
        public String formatValue(@NotNull Duration value) {
            return value.toSeconds() + " seconds";
        }

        @Override
        @NotNull
        public String getInvalidValueMessage(String value) {
            return "Auto balance duration must be a positive number of seconds.";
        }


}
