package me.mykindos.betterpvp.game.impl.domination.model.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.ServerController;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.framework.state.GameState;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the points per second when standing on a capture point in Domination.
 */
@Singleton
public class CapturePointScoreAttribute extends BoundAttribute<Integer> {

    @Inject
    public CapturePointScoreAttribute() {
        super("game.domination.capture-point-score", 8);
    }

    @Override
    public boolean isValidValue(Integer value) {
        // Must be positive or zero
        return value >= 0;
    }

    @Override
    @Nullable
    public Integer parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Capture point score must be a positive integer or zero.";
    }
}