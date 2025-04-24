package me.mykindos.betterpvp.game.framework.model.attribute.global;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.attribute.GameAttribute;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Attribute for configuring the starting countdown duration.
 */
@Singleton
public class StartingCountdownAttribute extends GameAttribute<Duration> {

    @Inject
    public StartingCountdownAttribute() {
        super("game.start-countdown", Duration.ofSeconds(30));
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
        return "Start countdown must be a positive number.";
    }
}
