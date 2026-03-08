package me.mykindos.betterpvp.core.interaction.timing;

import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.config.ConfigEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;

/**
 * Represents the number of inputs required to trigger an interaction.
 * Usually 1 (single click), but can be higher for multi-click abilities.
 */
public record InputCount(
    @NotNull ToIntFunction<InteractionContext> supplier,
    @Nullable String debugDescription
) {

    /** Single input required (default). */
    public static final InputCount ONE = new InputCount(ctx -> 1, "1");

    /**
     * Create a fixed input count.
     */
    public static InputCount of(int count) {
        if (count == 1) return ONE;
        return new InputCount(ctx -> count, String.valueOf(count));
    }

    /**
     * Create an input count from a config entry.
     */
    public static InputCount fromConfig(@NotNull ConfigEntry<Integer> config) {
        return new InputCount(
            ctx -> config.get(),
            "config:" + config.getKey()
        );
    }

    /**
     * Create a dynamic input count.
     */
    public static InputCount dynamic(@NotNull ToIntFunction<InteractionContext> supplier) {
        return new InputCount(supplier, "dynamic");
    }

    /**
     * Get the required input count.
     */
    public int get(@Nullable InteractionContext context) {
        return supplier.applyAsInt(context);
    }

    /**
     * Check if this requires multiple inputs.
     */
    public boolean requiresMultiple(@Nullable InteractionContext context) {
        return get(context) > 1;
    }

    @Override
    public String toString() {
        return "InputCount[" + debugDescription + "]";
    }
}
