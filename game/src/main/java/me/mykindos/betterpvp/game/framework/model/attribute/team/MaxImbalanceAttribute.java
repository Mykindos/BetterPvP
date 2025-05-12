package me.mykindos.betterpvp.game.framework.model.attribute.team;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the maximum imbalance for a team before it should autobalance
 */
@GameScoped
public class MaxImbalanceAttribute extends BoundAttribute<Integer> {

    @Inject
    public MaxImbalanceAttribute() {
        super("game.max-imbalance", 1);
    }

    @Override
    public boolean isValidValue(Integer value) {
        // Must be greater than or equal to 0
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
        return "Maximum imbalance must be greater than or equal to 0.";
    }
}