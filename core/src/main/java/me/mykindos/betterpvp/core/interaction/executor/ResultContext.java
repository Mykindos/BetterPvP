package me.mykindos.betterpvp.core.interaction.executor;

import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteraction;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteractionKey;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Context for handling an interaction result.
 * Contains all the data needed for the ResultHandler to process a result uniformly.
 *
 * @param entity             the living entity executing the interaction
 * @param actor              the interaction actor
 * @param interaction        the interaction that was executed
 * @param node               the chain node containing the interaction
 * @param state              the interaction state
 * @param chain              the interaction chain
 * @param context            the interaction context
 * @param result             the result of the interaction
 * @param itemInstance       the item instance (may be null)
 * @param itemStack          the item stack (may be null)
 * @param input              the input that triggered the interaction
 * @param executionId        the execution ID for state tracking
 * @param activeInteractions the map of active interactions (for Running result tracking)
 */
public record ResultContext(
        @NotNull LivingEntity entity,
        @NotNull InteractionActor actor,
        @NotNull Interaction interaction,
        @NotNull InteractionChainNode node,
        @NotNull InteractionState state,
        @NotNull InteractionChain chain,
        @NotNull InteractionContext context,
        @NotNull InteractionResult result,
        @Nullable ItemInstance itemInstance,
        @Nullable ItemStack itemStack,
        @NotNull InteractionInput input,
        long executionId,
        @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions
) {

    /**
     * Get the actor's unique ID.
     *
     * @return the actor's UUID
     */
    public UUID actorId() {
        return entity.getUniqueId();
    }
}
