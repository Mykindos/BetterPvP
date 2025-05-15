package me.mykindos.betterpvp.game.impl.domination.model.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the score required to win in Domination.
 */
@Singleton
public class ScoreToWinAttribute extends BoundAttribute<Integer> {

    @Inject
    public ScoreToWinAttribute() {
        super("game.domination.score-to-win", 15000);
    }

    @Override
    public boolean isValidValue(Integer value) {
        // Must be positive
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
        return "Score to win must be a positive integer.";
    }
}