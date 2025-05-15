package me.mykindos.betterpvp.game.framework.model.attribute.bound;

import com.google.inject.Inject;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import me.mykindos.betterpvp.game.guice.GameScoped;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the maximum number of players.
 */
@GameScoped
public class MaxPlayersAttribute extends BoundAttribute<Integer> {

    @Inject
    public MaxPlayersAttribute() {
        super("game.max-players", 16);
    }

    @Override
    public boolean isValidValue(Integer value) {
        // Must be positive and greater than or equal to required players
        return value > 0;
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
        return "Maximum players must be a positive integer.";
    }
}