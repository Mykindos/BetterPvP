package me.mykindos.betterpvp.game.impl.domination.model.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the points per gem in Domination.
 */
@Singleton
public class GemScoreAttribute extends BoundAttribute<Integer> {

    @Inject
    public GemScoreAttribute() {
        super("game.domination.gem-score", 300);
    }

    @Override
    public boolean isValidValue(Integer value) {
        // Must be non-negative
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
        return "Gem score must be a non-negative integer.";
    }
}