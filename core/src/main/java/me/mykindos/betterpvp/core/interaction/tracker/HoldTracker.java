package me.mykindos.betterpvp.core.interaction.tracker;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEndEvent;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.event.InteractionPostExecuteEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionPreExecuteEvent;
import me.mykindos.betterpvp.core.interaction.executor.InputRouter;
import me.mykindos.betterpvp.core.interaction.executor.InteractionExecutor;
import me.mykindos.betterpvp.core.interaction.executor.ResultHandler;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages held items and HOLD_RIGHT_CLICK lifecycle.
 */
@CustomLog
@BPvPListener
@Singleton
public class HoldTracker implements Listener {

    private final Map<UUID, HoldData> heldMap = new HashMap<>();
    private final Map<ActiveHoldKey, ActiveHold> activeHolds = new ConcurrentHashMap<>();

    private final ItemFactory itemFactory;
    private final InteractionExecutor executor;
    private final ActiveInteractionTracker activeInteractionTracker;

    @Inject
    public HoldTracker(ItemFactory itemFactory, InteractionExecutor executor,
                       ActiveInteractionTracker activeInteractionTracker) {
        this.itemFactory = itemFactory;
        this.executor = executor;
        this.activeInteractionTracker = activeInteractionTracker;
    }

    /**
     * Get the HoldData for an entity if they are holding a valid interaction item.
     */
    public Optional<HoldData> getHoldData(UUID entityId) {
        return Optional.ofNullable(heldMap.get(entityId));
    }

    /**
     * Remove hold data for an entity.
     */
    public void removeHoldData(UUID entityId) {
        heldMap.remove(entityId);
    }

    /**
     * Remove all active holds for the given actor.
     */
    public void removeActor(UUID actorId) {
        heldMap.remove(actorId);
        activeHolds.entrySet().removeIf(entry -> entry.getKey().actorId().equals(actorId));
    }

    /**
     * Update the held item for a living entity.
     */
    public void updateHeldItem(LivingEntity livingEntity, @Nullable ItemStack itemStack) {
        UUID entityId = livingEntity.getUniqueId();
        heldMap.remove(entityId);

        // Clean up any active holds for this entity when switching items
        cleanupActiveHolds(livingEntity);

        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return;
        }

        Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemStack);
        if (itemOpt.isEmpty()) {
            return;
        }

        ItemInstance item = itemOpt.get();
        Optional<InteractionContainerComponent> containerOpt = item.getComponent(InteractionContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        heldMap.put(entityId, new HoldData(livingEntity, item));
    }

    /**
     * Clean up any active holds for the given entity.
     * This triggers the end-of-hold lifecycle (LAST_RUN) for each active hold.
     */
    public void cleanupActiveHolds(LivingEntity livingEntity) {
        UUID entityId = livingEntity.getUniqueId();
        ItemStack itemStack = livingEntity.getEquipment() != null
                ? livingEntity.getEquipment().getItemInMainHand()
                : null;

        ResultHandler resultHandler = executor.getResultHandler();

        activeHolds.entrySet().removeIf(entry -> {
            if (!entry.getKey().actorId().equals(entityId)) {
                return false;
            }

            ActiveHold hold = entry.getValue();
            executeHoldEnd(livingEntity, hold, itemStack, resultHandler, false);
            return true;
        });
    }

    /**
     * Execute the end-of-hold lifecycle.
     */
    private void executeHoldEnd(LivingEntity entity, ActiveHold hold, ItemStack itemStack,
                                 ResultHandler resultHandler, boolean fireEvents) {
        InteractionChainNode targetNode = hold.getNode();
        Interaction interaction = targetNode.getInteraction();
        InteractionState state = hold.getState();
        InteractionContext context = state.getContext();
        InteractionActor actor = executor.createActor(entity);

        // Mark as last run
        context.remove(InputMeta.FIRST_RUN);
        context.set(InputMeta.LAST_RUN, true);

        // Fire pre-execute event if requested
        if (fireEvents) {
            InteractionPreExecuteEvent preEvent = new InteractionPreExecuteEvent(
                    actor, interaction, InteractionInputs.HOLD_RIGHT_CLICK, context, hold.getItemInstance(), itemStack);
            UtilServer.callEvent(preEvent);
            if (preEvent.isCancelled()) {
                context.remove(InputMeta.LAST_RUN);
                return;
            }
        }

        // Execute final tick
        InteractionResult result = interaction.execute(actor, context, hold.getItemInstance(), itemStack);

        // Fire post-execute event if requested
        if (fireEvents) {
            InteractionPostExecuteEvent postEvent = new InteractionPostExecuteEvent(
                    actor, interaction, InteractionInputs.HOLD_RIGHT_CLICK, context, result, hold.getItemInstance(), itemStack);
            UtilServer.callEvent(postEvent);
        }

        // Call then callback
        interaction.then(actor, context, result, hold.getItemInstance(), itemStack);

        // Clear LAST_RUN after then() so interactions can check it
        context.remove(InputMeta.LAST_RUN);

        // Execute follow-up interactions
        resultHandler.executeFollowUps(actor, targetNode, context, result, hold.getItemInstance(), itemStack);

        // Handle result
        handleHoldEndResult(entity, actor, hold, result, itemStack, resultHandler);
    }

    /**
     * Handle the result of a hold end execution.
     */
    private void handleHoldEndResult(LivingEntity entity, InteractionActor actor, ActiveHold hold,
                                      InteractionResult result, ItemStack itemStack, ResultHandler resultHandler) {
        if (result instanceof InteractionResult.Success success) {
            if (hold.getNode().getInteraction().consumesItem()) {
                UtilInventory.consumeHand(entity);
            }
            if (success.shouldAdvanceChain()) {
                resultHandler.advanceChain(entity, actor, hold.getNode(), hold.getState(), hold.getChain(),
                        hold.getState().getContext(), hold.getItemInstance(), itemStack, hold.getExecutionId(),
                        activeInteractionTracker.getActiveInteractions());
            }
        } else if (result instanceof InteractionResult.Fail) {
            resultHandler.recordChainCompletion(entity.getUniqueId(), hold.getState());
            hold.getState().reset();
        }
    }

    /**
     * Tick passive HOLD interactions for held items.
     */
    @UpdateEvent
    public void onHold() {
        heldMap.entrySet().removeIf(entry -> {
            final HoldData holdData = entry.getValue();
            LivingEntity entity = holdData.getEntity();
            if (entity == null || !entity.isValid()) {
                return true;
            }

            final Optional<InteractionContainerComponent> component = holdData.getInstance().getComponent(InteractionContainerComponent.class);
            if (component.isEmpty()) {
                return true;
            }

            final Optional<InteractionChainNode> root = component.get().findRoot(InteractionInputs.HOLD);
            if (root.isPresent()) {
                executor.processInput(entity, InteractionInputs.HOLD, holdData.getInstance(),
                        holdData.getInstance().getItemStack(), activeInteractionTracker.getActiveInteractions());
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(RightClickEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        HoldData data = heldMap.get(event.getPlayer().getUniqueId());
        if (data == null) {
            return;
        }

        ItemInstance itemInstance = data.getInstance();
        Optional<InteractionContainerComponent> containerOpt = itemInstance.getComponent(InteractionContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return;
        }

        InteractionChain chain = containerOpt.get().getChain();
        Player player = event.getPlayer();
        UUID actorId = player.getUniqueId();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        InteractionActor actor = executor.createActor(player);

        // Check if there's already an active hold for this player - if so, continue with that hold's state
        for (Map.Entry<ActiveHoldKey, ActiveHold> entry : activeHolds.entrySet()) {
            if (entry.getKey().actorId().equals(actorId)) {
                ActiveHold hold = entry.getValue();
                executeHoldTick(player, actor, hold.getNode(), hold.getState(), hold.getChain(),
                        hold.getItemInstance(), itemStack, true, false, hold.getExecutionId());
                return;
            }
        }

        // Use InputRouter to find the target
        InputRouter router = executor.getRouter();
        InputRouter.RouteResult routeResult = router.route(actorId, chain, InteractionInputs.HOLD_RIGHT_CLICK);

        switch (routeResult) {
            case InputRouter.RouteResult.NoMatch() -> {}
            case InputRouter.RouteResult.Consumed() -> {}
            case InputRouter.RouteResult.TargetFound(var node, var state, var execId, var isNew) -> {
                // Check root cooldown for new chains
                if (isNew && node.isRoot()) {
                    InteractionContext context = state.getContext();
                    if (!router.hasRootCooldownPassed(actorId, node, context)) {
                        router.removeState(actorId, chain,  execId);
                        return;
                    }
                }
                executeHoldTick(player, actor, node, state, chain, itemInstance, itemStack, false, isNew, execId);
            }
            case InputRouter.RouteResult.MultipleTargets(var nodes, var aid, var ch) -> {
                // For HOLD_RIGHT_CLICK with multiple roots, execute only the first one
                if (!nodes.isEmpty()) {
                    InteractionChainNode node = nodes.getFirst();
                    long execId = router.getStateManager().nextExecutionId();
                    InteractionState state = router.getStateManager().createState(actorId, chain, execId);
                    state.setRootNode(node);

                    InteractionContext context = state.getContext();
                    if (!router.hasRootCooldownPassed(actorId, node, context)) {
                        router.removeState(actorId, chain, execId);
                        return;
                    }
                    executeHoldTick(player, actor, node, state, chain, itemInstance, itemStack, false, true, execId);
                }
            }
        }
    }

    /**
     * Execute a single tick of a HOLD_RIGHT_CLICK interaction.
     */
    private void executeHoldTick(Player player, InteractionActor actor, InteractionChainNode targetNode,
                                 InteractionState state, InteractionChain chain, ItemInstance itemInstance,
                                 ItemStack itemStack, boolean isHoldClick, boolean isRoot, long executionId) {
        InteractionContext context = state.getContext();
        Interaction interaction = targetNode.getInteraction();
        ActiveHoldKey holdKey = new ActiveHoldKey(player.getUniqueId(), targetNode);

        boolean isFirstTick = !isHoldClick;

        if (isFirstTick) {
            // First tick of a new hold - set up context
            if (isRoot) {
                context.resetChain();
            }
            context.startExecution();
            context.set(InputMeta.FIRST_RUN, true);

            // Register this hold
            activeHolds.put(holdKey, new ActiveHold(player.getUniqueId(), targetNode, state, chain, itemInstance, executionId));
        } else {
            // Continuation tick - don't reset execution data, clear FIRST_RUN if present
            context.remove(InputMeta.FIRST_RUN);
        }

        // Fire pre-execute event
        InteractionPreExecuteEvent preEvent = new InteractionPreExecuteEvent(
                actor, interaction, InteractionInputs.HOLD_RIGHT_CLICK, context, itemInstance, itemStack);
        UtilServer.callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return;
        }

        // Execute
        InteractionResult result = interaction.execute(actor, context, itemInstance, itemStack);

        // Fire post-execute event
        InteractionPostExecuteEvent postEvent = new InteractionPostExecuteEvent(
                actor, interaction, InteractionInputs.HOLD_RIGHT_CLICK, context, result, itemInstance, itemStack);
        UtilServer.callEvent(postEvent);

        // Handle result (but don't advance chain until hold ends)
        if (result instanceof InteractionResult.Fail fail) {
            // Failed - clean up hold and reset
            activeHolds.remove(holdKey);
            interaction.then(actor, context, result, itemInstance, itemStack);
            executor.getResultHandler().executeFollowUps(actor, targetNode, context, result, itemInstance, itemStack);
            executor.getResultHandler().recordChainCompletion(player.getUniqueId(), state);
            log.debug("HOLD_RIGHT_CLICK interaction {} failed: {} - {}", interaction.getName(), fail.reason(), fail.message()).submit();
            state.reset();
        }
        // Success results are handled when the hold ends (RightClickEndEvent)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRightClickEnd(RightClickEndEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        ResultHandler resultHandler = executor.getResultHandler();

        // Find and process all active holds for this player
        activeHolds.entrySet().removeIf(entry -> {
            if (!entry.getKey().actorId().equals(playerId)) {
                return false;
            }

            ActiveHold hold = entry.getValue();
            executeHoldEnd(player, hold, itemStack, resultHandler, true);
            return true; // Remove the hold
        });
    }
}
