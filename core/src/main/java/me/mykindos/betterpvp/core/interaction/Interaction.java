package me.mykindos.betterpvp.core.interaction;

import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.condition.InteractionCondition;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base contract for all interactions.
 * An interaction represents an action that can be triggered by an input.
 */
public interface Interaction {

    /**
     * Get the display name of this interaction.
     *
     * @return the name
     */
    @NotNull
    String getName();

    /**
     * Get the description of this interaction.
     *
     * @return the description
     */
    @NotNull
    String getDescription();

    /**
     * Get the conditions that must be met for this interaction to execute.
     *
     * @return the list of conditions
     */
    @NotNull
    List<InteractionCondition> getConditions();

    /**
     * Execute this interaction.
     *
     * @param actor        the actor performing the interaction
     * @param context      the interaction context (for sharing data across chains)
     * @param itemInstance the item instance triggering the interaction (may be null)
     * @param itemStack    the item stack triggering the interaction (may be null)
     * @return the result of the interaction
     */
    @NotNull
    InteractionResult execute(
            @NotNull InteractionActor actor,
            @NotNull InteractionContext context,
            @Nullable ItemInstance itemInstance,
            @Nullable ItemStack itemStack
    );

    /**
     * Check if this interaction consumes the item when successfully executed.
     *
     * @return true if the item is consumed
     */
    default boolean consumesItem() {
        return false;
    }

    /**
     * Called after this interaction finishes executing, regardless of the outcome.
     * This is called for all completed interactions (success, fail, cancelled, timeout, etc.).
     * <p>
     * Override this method to perform cleanup, trigger follow-up actions, or log results.
     * The default implementation does nothing.
     *
     * @param actor        the actor who performed the interaction
     * @param context      the interaction context
     * @param result       the final result of the interaction
     * @param itemInstance the item instance (may be null)
     * @param itemStack    the item stack (may be null)
     */
    default void then(
            @NotNull InteractionActor actor,
            @NotNull InteractionContext context,
            @NotNull InteractionResult result,
            @Nullable ItemInstance itemInstance,
            @Nullable ItemStack itemStack
    ) {
        // Default implementation does nothing
    }

}
