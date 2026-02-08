package me.mykindos.betterpvp.core.interaction.tracker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.event.InteractionChainCompleteEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionChainTimeoutEvent;
import me.mykindos.betterpvp.core.interaction.executor.InteractionExecutor;
import me.mykindos.betterpvp.core.interaction.executor.ResultHandler;
import me.mykindos.betterpvp.core.interaction.state.InteractionStateManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages running (duration-based) interactions that need to be ticked.
 */
@CustomLog
@BPvPListener
@Singleton
public class ActiveInteractionTracker implements Listener {

    private final Map<ActiveInteractionKey, ActiveInteraction> activeInteractions = new ConcurrentHashMap<>();
    private final InteractionExecutor executor;
    private final InteractionStateManager stateManager;

    @Inject
    public ActiveInteractionTracker(InteractionExecutor executor, InteractionStateManager stateManager) {
        this.executor = executor;
        this.stateManager = stateManager;
    }

    /**
     * Get the map of active interactions.
     * Used by InteractionExecutor to register Running interactions.
     */
    public Map<ActiveInteractionKey, ActiveInteraction> getActiveInteractions() {
        return activeInteractions;
    }

    /**
     * Check if an interaction is already running for the given key.
     */
    public boolean isRunning(ActiveInteractionKey key) {
        return activeInteractions.containsKey(key);
    }

    /**
     * Remove all active interactions for the given actor.
     */
    public void removeActor(UUID actorId) {
        activeInteractions.entrySet().removeIf(entry -> entry.getKey().actorId().equals(actorId));
    }

    /**
     * Tick all active (running) interactions.
     */
    @UpdateEvent
    public void tickActiveInteractions() {
        long currentTick = Bukkit.getCurrentTick();
        long currentTime = System.currentTimeMillis();

        Iterator<Map.Entry<ActiveInteractionKey, ActiveInteraction>> iterator = activeInteractions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ActiveInteractionKey, ActiveInteraction> entry = iterator.next();
            ActiveInteraction active = entry.getValue();

            // Try to find the entity
            LivingEntity entity = findEntity(active.getActorId());
            if (entity == null) {
                iterator.remove();
                continue;
            }

            // Handle invalid entities (dead, removed, etc.) - cleanup with failure result
            if (!entity.isValid()) {
                iterator.remove();
                handleInvalidEntity(entity, active);
                continue;
            }

            // Check timeout
            if (currentTime - active.getStartTime() > active.getMaxRuntimeMillis()) {
                iterator.remove();
                handleTimeout(entity, active);
                continue;
            }

            // Check interval
            if (currentTick - active.getLastExecutionTick() < active.getIntervalTicks()) {
                continue;
            }

            // Execute tick
            tickInteraction(entity, active, currentTime, currentTick);
        }
    }

    /**
     * Find a living entity by UUID.
     */
    private LivingEntity findEntity(UUID actorId) {
        // Try player first
        LivingEntity entity = Bukkit.getPlayer(actorId);
        if (entity == null) {
            // Fallback to entity lookup
            Entity found = Bukkit.getEntity(actorId);
            if (found instanceof LivingEntity livingEntity) {
                entity = livingEntity;
            }
        }
        return entity;
    }

    /**
     * Handle an invalid entity (dead, removed, etc.) - cleanup with failure result.
     */
    private void handleInvalidEntity(LivingEntity entity, ActiveInteraction active) {
        InteractionActor actor = executor.createActor(entity);
        InteractionContext context = active.getContext();
        InteractionResult.Fail failResult = new InteractionResult.Fail(InteractionResult.FailReason.CANCELLED, "Entity invalid");

        // Call then callback for cleanup
        active.getInteraction().then(actor, context, failResult, active.getItemInstance(), active.getItemStack());

        // Execute follow-up interactions
        ResultHandler resultHandler = executor.getResultHandler();
        resultHandler.executeFollowUps(actor, active.getNode(), context, failResult,
                active.getItemInstance(), active.getItemStack());

        // Record chain completion and reset state
        resultHandler.recordChainCompletion(active.getActorId(), active.getState());
        active.getState().reset();
    }

    /**
     * Handle timeout for an active interaction.
     */
    private void handleTimeout(LivingEntity entity, ActiveInteraction active) {
        InteractionActor actor = executor.createActor(entity);
        ResultHandler resultHandler = executor.getResultHandler();

        if (active.isGracefulTimeout()) {
            handleGracefulTimeout(entity, actor, active, resultHandler);
        } else {
            handleHardTimeout(entity, actor, active, resultHandler);
        }
    }

    /**
     * Handle graceful timeout - completes as success.
     */
    private void handleGracefulTimeout(LivingEntity entity, InteractionActor actor,
                                        ActiveInteraction active, ResultHandler resultHandler) {
        InteractionResult.Success successResult = InteractionResult.Success.ADVANCE;
        InteractionContext context = active.getContext();

        // Call then callback
        active.getInteraction().then(actor, context, successResult, active.getItemInstance(), active.getItemStack());

        // Execute follow-up interactions
        resultHandler.executeFollowUps(actor, active.getNode(), context, successResult,
                active.getItemInstance(), active.getItemStack());

        // Handle item consumption
        if (active.getInteraction().consumesItem()) {
            UtilInventory.consumeHand(entity);
        }

        // Fire completion event
        InteractionChainCompleteEvent completeEvent = new InteractionChainCompleteEvent(
                actor, active.getChain().getId(), active.getNode(), context);
        UtilServer.callEvent(completeEvent);

        if (entity instanceof Player player) {
            player.clearActiveItem();
        }

        // Advance chain if there are children, otherwise reset
        if (active.getNode().hasChildren()) {
            active.getState().advanceTo(active.getNode());
        } else {
            resultHandler.recordChainCompletion(active.getActorId(), active.getState());
            active.getState().reset();
        }
    }

    /**
     * Handle hard timeout - completes as failure.
     */
    private void handleHardTimeout(LivingEntity entity, InteractionActor actor,
                                    ActiveInteraction active, ResultHandler resultHandler) {
        InteractionResult.Fail timeoutResult = new InteractionResult.Fail(InteractionResult.FailReason.TIMEOUT);
        InteractionContext context = active.getContext();

        // Call then callback
        active.getInteraction().then(actor, context, timeoutResult, active.getItemInstance(), active.getItemStack());

        // Execute follow-up interactions
        resultHandler.executeFollowUps(actor, active.getNode(), context, timeoutResult,
                active.getItemInstance(), active.getItemStack());

        // Fire timeout event
        InteractionChainTimeoutEvent timeoutEvent = new InteractionChainTimeoutEvent(
                actor, active.getChain().getId(), active.getNode(), context);
        UtilServer.callEvent(timeoutEvent);

        // Record chain completion and reset
        resultHandler.recordChainCompletion(active.getActorId(), active.getState());
        active.getState().reset();
    }

    /**
     * Tick a single active interaction.
     */
    private void tickInteraction(LivingEntity entity, ActiveInteraction active, long currentTime, long currentTick) {
        InteractionActor actor = executor.createActor(entity);
        InteractionContext context = active.getContext();
        ResultHandler resultHandler = executor.getResultHandler();

        // Clear FIRST_RUN since this is not the first execution
        context.remove(InputMeta.FIRST_RUN);

        // Check if this is the last execution before timeout
        long nextTickTime = currentTime + (active.getIntervalTicks() * 50L);
        boolean isLastRun = (nextTickTime - active.getStartTime()) > active.getMaxRuntimeMillis();
        if (isLastRun) {
            context.set(InputMeta.LAST_RUN, true);
        }

        // Execute the interaction
        InteractionResult result = active.getInteraction().execute(
                actor, context, active.getItemInstance(), active.getItemStack());

        // Update last execution tick
        active.setLastExecutionTick(currentTick);

        // Handle result
        if (result.isComplete()) {
            handleCompleteResult(entity, actor, active, result, resultHandler);
        } else if (result instanceof InteractionResult.Running running) {
            // Allow interaction to update interval/runtime/gracefulTimeout dynamically
            active.setIntervalTicks(running.intervalTicks());
            active.setMaxRuntimeMillis(running.maxRuntimeMillis());
            active.setGracefulTimeout(running.gracefulTimeout());
        }
    }

    /**
     * Handle a complete result from a ticked interaction.
     */
    private void handleCompleteResult(LivingEntity entity, InteractionActor actor,
                                       ActiveInteraction active, InteractionResult result,
                                       ResultHandler resultHandler) {
        activeInteractions.remove(new ActiveInteractionKey(active.getActorId(), active.getNode()));
        InteractionContext context = active.getContext();

        // Call then callback
        active.getInteraction().then(actor, context, result, active.getItemInstance(), active.getItemStack());

        // Clear running interaction meta keys after then() so interactions can check them
        context.remove(InputMeta.LAST_RUN);

        // Execute follow-up interactions
        resultHandler.executeFollowUps(actor, active.getNode(), context, result,
                active.getItemInstance(), active.getItemStack());

        if (result instanceof InteractionResult.Success success) {
            // Handle item consumption
            if (active.getInteraction().consumesItem()) {
                UtilInventory.consumeHand(entity);
            }

            // Advance chain if successful
            if (success.shouldAdvanceChain()) {
                resultHandler.advanceChain(entity, actor, active.getNode(), active.getState(), active.getChain(),
                        context, active.getItemInstance(), active.getItemStack(),
                        active.getExecutionId(), activeInteractions);
            } else {
                // NO_ADVANCE after Running interaction - chain should end
                resultHandler.recordChainCompletion(active.getActorId(), active.getState());
                active.getState().reset();
            }
        } else if (result instanceof InteractionResult.Fail) {
            // Failed during execution - reset state
            resultHandler.recordChainCompletion(active.getActorId(), active.getState());
            active.getState().reset();
        }
    }
}
