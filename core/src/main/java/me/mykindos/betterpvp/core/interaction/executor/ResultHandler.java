package me.mykindos.betterpvp.core.interaction.executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.event.InteractionChainCompleteEvent;
import me.mykindos.betterpvp.core.interaction.followup.InteractionFollowUps;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.interaction.state.InteractionStateManager;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteraction;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteractionKey;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Handles interaction results uniformly across all execution paths.
 * Consolidates result handling logic from InteractionExecutor, ActiveInteractionTracker, and HoldTracker.
 */
@CustomLog
@Singleton
public class ResultHandler {

    private final InteractionStateManager stateManager;

    @Inject
    public ResultHandler(InteractionStateManager stateManager) {
        this.stateManager = stateManager;
    }

    /**
     * Handle an interaction result.
     *
     * @param ctx the result context containing all data needed for handling
     * @return true if the interaction was successful (including starting a running interaction)
     */
    public boolean handle(@NotNull ResultContext ctx) {
        return switch (ctx.result()) {
            case InteractionResult.Success success -> handleSuccess(ctx, success);
            case InteractionResult.Running running -> handleRunning(ctx, running);
            case InteractionResult.Fail fail -> handleFail(ctx, fail);
        };
    }

    /**
     * Handle a successful interaction result.
     */
    private boolean handleSuccess(@NotNull ResultContext ctx, @NotNull InteractionResult.Success success) {
        InteractionContext context = ctx.context();
        Interaction interaction = ctx.interaction();

        // Clear FIRST_RUN since we're done with this execution
        context.remove(InputMeta.FIRST_RUN);

        // Call then callback
        interaction.then(ctx.actor(), context, success, ctx.itemInstance(), ctx.itemStack());

        // Execute follow-up interactions
        executeFollowUps(ctx.actor(), ctx.node(), context, success, ctx.itemInstance(), ctx.itemStack());

        // Handle item consumption
        if (interaction.consumesItem()) {
            UtilInventory.consumeHand(ctx.entity());
        }

        // Advance chain if needed
        if (success.shouldAdvanceChain()) {
            advanceChain(ctx);
        } else {
            // Success with NO_ADVANCE - chain should end
            completeChain(ctx);
        }

        return true;
    }

    /**
     * Handle a running interaction result by registering it for ticking.
     */
    private boolean handleRunning(@NotNull ResultContext ctx, @NotNull InteractionResult.Running running) {
        ActiveInteractionKey key = new ActiveInteractionKey(ctx.actorId(), ctx.node());

        ActiveInteraction active = new ActiveInteraction(
                ctx.actorId(),
                ctx.actor(),
                ctx.interaction(),
                ctx.node(),
                ctx.state(),
                ctx.chain(),
                ctx.context(),
                ctx.itemInstance(),
                ctx.itemStack(),
                ctx.input(),
                ctx.executionId(),
                System.currentTimeMillis(),
                Bukkit.getCurrentTick(),
                running.intervalTicks(),
                running.maxRuntimeMillis(),
                running.gracefulTimeout()
        );

        ctx.activeInteractions().put(key, active);
        return true;
    }

    /**
     * Handle a failed interaction result.
     */
    private boolean handleFail(@NotNull ResultContext ctx, @NotNull InteractionResult.Fail fail) {
        InteractionContext context = ctx.context();
        Interaction interaction = ctx.interaction();

        // Clear FIRST_RUN since we're done with this execution
        context.remove(InputMeta.FIRST_RUN);

        // Call then callback
        interaction.then(ctx.actor(), context, fail, ctx.itemInstance(), ctx.itemStack());

        // Execute follow-up interactions
        executeFollowUps(ctx.actor(), ctx.node(), context, fail, ctx.itemInstance(), ctx.itemStack());

        // Record chain completion for root cooldown tracking using the root node's ID
        recordChainCompletion(ctx.actorId(), ctx.state());

        // Failed - reset chain state and clear chain data
        ctx.state().reset();
        log.debug("Interaction {} failed: {} - {}", interaction.getName(), fail.reason(), fail.message()).submit();

        return false;
    }

    /**
     * Advance the chain to the next node or complete it.
     * If the next node has a NONE input, it will be executed immediately.
     */
    public void advanceChain(@NotNull ResultContext ctx) {
        advanceChain(ctx.entity(), ctx.actor(), ctx.node(), ctx.state(), ctx.chain(),
                ctx.context(), ctx.itemInstance(), ctx.itemStack(), ctx.executionId(), ctx.activeInteractions());
    }

    /**
     * Advance the chain to the next node or complete it.
     * If the next node has a NONE input, it will be executed immediately.
     *
     * @param entity             the living entity
     * @param actor              the interaction actor
     * @param targetNode         the current node that was just executed
     * @param state              the interaction state
     * @param chain              the interaction chain
     * @param context            the interaction context
     * @param itemInstance       the item instance
     * @param itemStack          the item stack
     * @param executionId        the execution ID
     * @param activeInteractions the map of active interactions
     */
    public void advanceChain(@NotNull LivingEntity entity, @NotNull InteractionActor actor,
                             @NotNull InteractionChainNode targetNode, @NotNull InteractionState state,
                             @NotNull InteractionChain chain, @NotNull InteractionContext context,
                             @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack,
                             long executionId, @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions) {
        if (targetNode.hasChildren()) {
            state.advanceTo(targetNode);

            // Check if there's a child with NONE input - execute it immediately
            targetNode.findChild(InteractionInputs.NONE).ifPresent(noneNode -> {
                // Create an execution pipeline to execute the NONE child
                // This maintains the chain continuation semantics
                executeNoneChild(entity, actor, noneNode, state, chain, itemInstance, itemStack, executionId, activeInteractions);
            });
        } else {
            // Chain complete - record completion for root cooldown tracking
            recordChainCompletion(entity.getUniqueId(), state);

            // Fire event and reset
            InteractionChainCompleteEvent completeEvent = new InteractionChainCompleteEvent(
                    actor, chain.getId(), targetNode, context);
            UtilServer.callEvent(completeEvent);
            state.reset();
        }
    }

    /**
     * Execute a child node with NONE input.
     * This is an internal method for chain continuation and creates its own pipeline execution.
     */
    private void executeNoneChild(@NotNull LivingEntity entity, @NotNull InteractionActor actor,
                                  @NotNull InteractionChainNode noneNode, @NotNull InteractionState state,
                                  @NotNull InteractionChain chain, @Nullable ItemInstance itemInstance,
                                  @Nullable ItemStack itemStack, long executionId,
                                  @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions) {
        // For NONE children, we need to execute directly since this is an internal chain continuation
        // The execution pipeline will be used by the caller if needed
        InteractionContext context = state.getContext();

        // Start a new execution for the NONE child
        context.startExecution();
        context.set(InputMeta.FIRST_RUN, true);

        // Execute the NONE child interaction
        Interaction interaction = noneNode.getInteraction();
        InteractionResult result = interaction.execute(actor, context, itemInstance, itemStack);

        // Handle the result
        ResultContext resultCtx = new ResultContext(entity, actor, interaction, noneNode, state, chain,
                context, result, itemInstance, itemStack, InteractionInputs.NONE, executionId, activeInteractions);
        handle(resultCtx);
    }

    /**
     * Complete the chain, recording the completion for cooldown tracking.
     */
    public void completeChain(@NotNull ResultContext ctx) {
        recordChainCompletion(ctx.actorId(), ctx.state());
        ctx.state().reset();
    }

    /**
     * Record chain completion for root cooldown tracking.
     *
     * @param actorId the actor's UUID
     * @param state   the interaction state
     */
    public void recordChainCompletion(@NotNull UUID actorId, @NotNull InteractionState state) {
        InteractionChainNode rootNode = state.getRootNode();
        if (rootNode != null) {
            stateManager.recordChainCompletion(actorId, rootNode.getId());
        }
    }

    /**
     * Execute follow-up interactions for a completed node.
     *
     * @param actor        the actor
     * @param node         the node that completed
     * @param context      the interaction context
     * @param result       the result of the parent interaction
     * @param itemInstance the item instance (may be null)
     * @param itemStack    the item stack (may be null)
     */
    public void executeFollowUps(@NotNull InteractionActor actor, @NotNull InteractionChainNode node,
                                 @NotNull InteractionContext context, @NotNull InteractionResult result,
                                 @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!node.hasFollowUps()) {
            return;
        }

        InteractionFollowUps followUps = node.getFollowUps();

        // Execute always (then) follow-ups first
        for (Interaction followUp : followUps.getAlways()) {
            followUp.execute(actor, context, itemInstance, itemStack);
        }

        // Execute condition-specific follow-ups
        if (result.isSuccess()) {
            for (Interaction followUp : followUps.getOnComplete()) {
                followUp.execute(actor, context, itemInstance, itemStack);
            }
        } else {
            for (Interaction followUp : followUps.getOnFail()) {
                followUp.execute(actor, context, itemInstance, itemStack);
            }
        }
    }
}
