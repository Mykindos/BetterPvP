package me.mykindos.betterpvp.game.impl.domination.model.attribute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.game.framework.model.attribute.BoundAttribute;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Attribute for configuring the seconds to capture a point in Domination.
 */
@Singleton
public class SecondsToCaptureAttribute extends BoundAttribute<Float> {

    @Inject
    public SecondsToCaptureAttribute() {
        super("game.domination.seconds-to-capture", 10.0f);
    }

    @Override
    public boolean isValidValue(Float value) {
        // Must be positive
        return value > 0;
    }

    @Override
    @Nullable
    public Float parseValue(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public String formatValue(@NotNull Float value) {
        return value + " seconds";
    }

    @Override
    @NotNull
    public String getInvalidValueMessage(String value) {
        return "Seconds to capture must be a positive number.";
    }
}