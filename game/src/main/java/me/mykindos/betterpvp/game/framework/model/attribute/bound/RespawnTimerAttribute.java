package me.mykindos.betterpvp.game.framework.model.attribute.bound;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.framework.state.GameState;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the respawn timer.
 */
@GameScoped
public class RespawnTimerAttribute extends BoundAttribute<Double> {

    @Inject
    public RespawnTimerAttribute() {
        super("game.respawn-seconds", 10.0);
    }

    @Override
    public boolean isValidValue(Double value) {
        // Must be positive
        return value > 0;
    }

    @Override
    @Nullable
    public Double parseValue(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public String formatValue(@NotNull Double value) {
        return value + " seconds";
    }

    @Override
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Respawn timer must be a positive number.";
    }
}
