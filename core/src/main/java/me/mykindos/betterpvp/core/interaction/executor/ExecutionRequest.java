package me.mykindos.betterpvp.core.interaction.executor;

import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Request for executing an interaction.
 * Contains all the data needed for the ExecutionPipeline to execute an interaction.
 *
 * @param entity             the living entity executing the interaction
 * @param actor              the interaction actor
 * @param node               the chain node containing the interaction
 * @param state              the interaction state
 * @param chain              the interaction chain
 * @param itemInstance       the item instance (may be null)
 * @param itemStack          the item stack (may be null)
 * @param input              the input that triggered the interaction
 * @param executionId        the execution ID for state tracking
 * @param isNewChain         whether this is starting a new chain (root node, not continuing)
 * @param setupContext       whether to set up context (resetChain, startExecution, FIRST_RUN)
 * @param executionDataSetup optional callback to set execution-scoped data after startExecution()
 */
public record ExecutionRequest(
        @NotNull LivingEntity entity,
        @NotNull InteractionActor actor,
        @NotNull InteractionChainNode node,
        @NotNull InteractionState state,
        @NotNull InteractionChain chain,
        @Nullable ItemInstance itemInstance,
        @Nullable ItemStack itemStack,
        @NotNull InteractionInput input,
        long executionId,
        boolean isNewChain,
        boolean setupContext,
        @Nullable Consumer<InteractionContext> executionDataSetup
) {

    /**
     * Get the interaction from the node.
     *
     * @return the interaction
     */
    public Interaction interaction() {
        return node.getInteraction();
    }

    /**
     * Get the interaction context from the state.
     *
     * @return the interaction context
     */
    public InteractionContext context() {
        return state.getContext();
    }

    /**
     * Create a request with default setup options.
     */
    public static ExecutionRequest of(
            @NotNull LivingEntity entity,
            @NotNull InteractionActor actor,
            @NotNull InteractionChainNode node,
            @NotNull InteractionState state,
            @NotNull InteractionChain chain,
            @Nullable ItemInstance itemInstance,
            @Nullable ItemStack itemStack,
            @NotNull InteractionInput input,
            long executionId,
            boolean isNewChain,
            @Nullable Consumer<InteractionContext> executionDataSetup
    ) {
        return new ExecutionRequest(entity, actor, node, state, chain, itemInstance, itemStack,
                input, executionId, isNewChain, true, executionDataSetup);
    }
}
