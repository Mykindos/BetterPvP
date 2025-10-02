package me.mykindos.betterpvp.core.loot;

import lombok.Builder;
import lombok.Data;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;

import java.util.function.Predicate;

/**
 * Represents a loot table entry. Loot must be able to be awarded to a {@link LootContext}
 * and its container {@link LootTable} must respect its {@link ReplacementStrategy} and
 * {@link Predicate} conditions.
 *
 * @param <T> The type of the loot.
 * @param <R> The type of the awarded reward. This is not guaranteed to be the same as the type of the loot.
 *           As some rewards have an in-world type representations.
 */
@Data
public abstract class Loot<T, R> {

    /**
     * The reward for this loot.
     */
    private final T reward;

    /**
     * The strategy for replacing existing loot.
     */
    private final ReplacementStrategy replacementStrategy;

    /**
     * The condition for this loot. If the condition is not met, the loot will not be awarded.
     */
    private final Predicate<LootContext> condition;

    /**
     * Awards this loot to the given context.
     * @param context The context to award the loot to.
     */
    public abstract R award(LootContext context);

    /**
     * Gets the icon for this loot in a menu
     * @return The icon for this loot.
     */
    public abstract ItemView getIcon();

    public abstract String toString();

}
