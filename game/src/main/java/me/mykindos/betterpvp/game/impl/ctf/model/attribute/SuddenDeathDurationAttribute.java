package me.mykindos.betterpvp.game.impl.ctf.model.attribute;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Attribute for configuring the sudden death duration in CTF.
 */
@GameScoped
public class SuddenDeathDurationAttribute extends BoundAttribute<Duration> {

    @Inject
    public SuddenDeathDurationAttribute() {
        super("game.ctf.suddenDeathDuration", Duration.ofMinutes(3));
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
        return "Sudden death duration must be a positive number of minutes.";
    }
}