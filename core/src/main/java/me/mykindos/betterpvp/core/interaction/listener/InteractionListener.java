package me.mykindos.betterpvp.core.interaction.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import me.mykindos.betterpvp.core.block.SmartBlockFactory;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.click.events.RightClickEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.EntityInteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.PlayerInteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.event.InteractionChainCompleteEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionChainTimeoutEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionPostExecuteEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionPreExecuteEvent;
import me.mykindos.betterpvp.core.interaction.followup.InteractionFollowUps;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.interaction.state.InteractionStateManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilInventory;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listener that captures inputs and routes them to the interaction system.
 * Also handles ticking of running (duration-based) interactions.
 */
@CustomLog
@BPvPListener
@Singleton
public class InteractionListener implements Listener {

    private final Map<UUID, HoldData> heldMap = new HashMap<>();
    private final Map<ActiveInteractionKey, ActiveInteraction> activeInteractions = new ConcurrentHashMap<>();

    private final ClientManager clientManager;
    private final ItemFactory itemFactory;
    private final SmartBlockFactory smartBlockFactory;
    private final InteractionStateManager stateManager;
    private final EnergyService energyService;
    private final EffectManager effectManager;

    @Inject
    public InteractionListener(ClientManager clientManager, ItemFactory itemFactory,
                                SmartBlockFactory smartBlockFactory, InteractionStateManager stateManager,
                                EnergyService energyService, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
        this.smartBlockFactory = smartBlockFactory;
        this.stateManager = stateManager;
        this.energyService = energyService;
        this.effectManager = effectManager;
    }

    /**
     * Create an appropriate InteractionActor for the given entity.
     *
     * @param entity the living entity
     * @return a PlayerInteractionActor for players, EntityInteractionActor otherwise
     */
    private InteractionActor createActor(@NotNull LivingEntity entity) {
        if (entity instanceof Player player) {
            Client client = clientManager.search().online(player);
            return new PlayerInteractionActor(player, client, energyService, effectManager);
        }
        return new EntityInteractionActor(entity, effectManager);
    }

    /**
     * Process an input for a living entity with an item.
     *
     * @param livingEntity the living entity
     * @param input        the input type
     * @param itemInstance the item instance
     * @param itemStack    the item stack
     * @return true if an interaction was triggered
     */
    private boolean processInput(@NotNull LivingEntity livingEntity, @NotNull InteractionInput input,
                                  @NotNull ItemInstance itemInstance, @NotNull ItemStack itemStack) {
        Optional<InteractionContainerComponent> containerOpt = itemInstance.getComponent(InteractionContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return false;
        }

        InteractionContainerComponent container = containerOpt.get();
        InteractionChain chain = container.getChain();

        InteractionActor actor = createActor(livingEntity);
        InteractionState state = stateManager.getOrCreateState(livingEntity.getUniqueId(), chain);

        // Check if we're continuing a chain or starting fresh
        InteractionChainNode targetNode;
        if (state.isInChain()) {
            // Check for timeout
            if (state.hasTimedOut()) {
                state.reset();
            }

            // Look for matching child
            InteractionChainNode currentNode = state.getCurrentNode();
            if (currentNode != null) {
                Optional<InteractionChainNode> childOpt = currentNode.findChild(input);
                if (childOpt.isPresent()) {
                    targetNode = childOpt.get();
                } else {
                    // No matching child - reset and try from root
                    state.reset();
                    Optional<InteractionChainNode> rootOpt = chain.findRoot(input);
                    if (rootOpt.isEmpty()) {
                        return false;
                    }
                    targetNode = rootOpt.get();
                }
            } else {
                // State says in chain but no current node - reset
                state.reset();
                Optional<InteractionChainNode> rootOpt = chain.findRoot(input);
                if (rootOpt.isEmpty()) {
                    return false;
                }
                targetNode = rootOpt.get();
            }
        } else {
            // Not in a chain - look for matching root
            Optional<InteractionChainNode> rootOpt = chain.findRoot(input);
            if (rootOpt.isEmpty()) {
                return false;
            }
            targetNode = rootOpt.get();
        }

        // Handle multi-input requirements
        if (targetNode.requiresMultipleInputs()) {
            int count = state.incrementInputCounter();
            if (count < targetNode.getRequiredInputCount()) {
                state.touch();
                return false; // Need more inputs
            }
        }

        // Execute the interaction
        return executeInteraction(livingEntity, actor, targetNode, state, chain, itemInstance, itemStack, input);
    }

    /**
     * Execute an interaction and handle the result.
     */
    private boolean executeInteraction(@NotNull LivingEntity entity, @NotNull InteractionActor actor,
                                        @NotNull InteractionChainNode targetNode, @NotNull InteractionState state,
                                        @NotNull InteractionChain chain, @NotNull ItemInstance itemInstance,
                                        @NotNull ItemStack itemStack, @NotNull InteractionInput input) {
        InteractionContext context = state.getContext();
        Interaction interaction = targetNode.getInteraction();

        // Fire pre-execute event
        InteractionPreExecuteEvent preEvent = new InteractionPreExecuteEvent(
                actor, interaction, input, context, itemInstance, itemStack);
        UtilServer.callEvent(preEvent);
        if (preEvent.isCancelled()) {
            return false;
        }

        // Check if this interaction is not already running - if so, this is the first execution
        ActiveInteractionKey key = new ActiveInteractionKey(entity.getUniqueId(), chain.getId(), interaction.getName());
        if (activeInteractions.containsKey(key)) {
            return false; // This interaction is already running
        }
        context.set(InputMeta.FIRST_RUN, true);

        // Execute
        InteractionResult result = interaction.execute(actor, context, itemInstance, itemStack);

        // Fire post-execute event
        InteractionPostExecuteEvent postEvent = new InteractionPostExecuteEvent(
                actor, interaction, input, context, result, itemInstance, itemStack);
        UtilServer.callEvent(postEvent);

        // Handle the result
        return handleResult(entity, actor, result, interaction, targetNode, state, chain, context, itemInstance, itemStack, input);
    }

    /**
     * Handle an interaction result.
     *
     * @return true if the interaction was successful (including starting a running interaction)
     */
    private boolean handleResult(@NotNull LivingEntity entity, @NotNull InteractionActor actor,
                                  @NotNull InteractionResult result, @NotNull Interaction interaction,
                                  @NotNull InteractionChainNode targetNode, @NotNull InteractionState state,
                                  @NotNull InteractionChain chain, @NotNull InteractionContext context,
                                  @NotNull ItemInstance itemInstance, @NotNull ItemStack itemStack,
                                  @NotNull InteractionInput input) {

        switch (result) {
            case InteractionResult.Success success -> {
                // Clear FIRST_RUN since we're done with this execution
                context.remove(InputMeta.FIRST_RUN);

                // Call then callback
                interaction.then(actor, context, result, itemInstance, itemStack);

                // Execute follow-up interactions
                executeFollowUps(actor, targetNode, context, result, itemInstance, itemStack);

                // Handle item consumption
                if (interaction.consumesItem()) {
                    UtilInventory.consumeHand(entity);
                }

                // Advance chain if needed
                if (success.shouldAdvanceChain()) {
                    advanceChain(actor, targetNode, state, chain, context);
                }

                return true;
            }
            case InteractionResult.Running running -> {
                // Start tracking this as an active interaction
                ActiveInteractionKey key = new ActiveInteractionKey(entity.getUniqueId(), chain.getId(), interaction.getName());

                ActiveInteraction active = new ActiveInteraction(
                        entity.getUniqueId(),
                        actor,
                        interaction,
                        targetNode,
                        state,
                        chain,
                        context,
                        itemInstance,
                        itemStack,
                        input,
                        System.currentTimeMillis(),
                        Bukkit.getCurrentTick(),
                        running.intervalTicks(),
                        running.maxRuntimeMillis(),
                        running.gracefulTimeout()
                );

                activeInteractions.put(key, active);
                return true;
            }
            case InteractionResult.Fail fail -> {
                // Clear FIRST_RUN since we're done with this execution
                context.remove(InputMeta.FIRST_RUN);

                // Call then callback
                interaction.then(actor, context, result, itemInstance, itemStack);

                // Execute follow-up interactions
                executeFollowUps(actor, targetNode, context, result, itemInstance, itemStack);

                // Failed - log if needed for debugging
                log.debug("Interaction {} failed: {} - {}", interaction.getName(), fail.reason(), fail.message()).submit();
                return false;
            }
            default -> {
            }
        }

        return false;
    }

    /**
     * Advance the chain to the next node or complete it.
     */
    private void advanceChain(@NotNull InteractionActor actor, @NotNull InteractionChainNode targetNode,
                               @NotNull InteractionState state, @NotNull InteractionChain chain,
                               @NotNull InteractionContext context) {
        if (targetNode.hasChildren()) {
            state.advanceTo(targetNode);
        } else {
            // Chain complete - fire event and reset
            InteractionChainCompleteEvent completeEvent = new InteractionChainCompleteEvent(
                    actor, chain.getId(), targetNode, context);
            UtilServer.callEvent(completeEvent);
            state.reset();
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
    private void executeFollowUps(@NotNull InteractionActor actor, @NotNull InteractionChainNode node,
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

            // Try to find the entity - player first, then fallback to entity lookup
            LivingEntity entity = Bukkit.getPlayer(active.getActorId());
            if (entity == null) {
                // Fallback to entity lookup by UUID across worlds
                // todo: this might be costly, could we possibly store a weak ref to the entity instead?
                final Entity found = Bukkit.getEntity(active.getActorId());
                if (found instanceof LivingEntity livingEntity) {
                    entity = livingEntity;
                }
            }

            if (entity == null || !entity.isValid()) {
                iterator.remove();
                continue;
            }

            // Check timeout
            if (currentTime - active.getStartTime() > active.getMaxRuntimeMillis()) {
                iterator.remove();

                // Refresh actor in case entity state changed
                InteractionActor actor = createActor(entity);

                if (active.isGracefulTimeout()) {
                    // Graceful timeout - complete as success
                    InteractionResult.Success successResult = InteractionResult.Success.ADVANCE;
                    active.getInteraction().then(actor, active.getContext(), successResult,
                            active.getItemInstance(), active.getItemStack());

                    // Execute follow-up interactions
                    executeFollowUps(actor, active.getNode(), active.getContext(), successResult,
                            active.getItemInstance(), active.getItemStack());

                    // Handle item consumption
                    if (active.getInteraction().consumesItem()) {
                        UtilInventory.consumeHand(entity);
                    }

                    // Fire completion event and advance chain
                    InteractionChainCompleteEvent completeEvent = new InteractionChainCompleteEvent(
                            actor, active.getChain().getId(), active.getNode(), active.getContext());
                    UtilServer.callEvent(completeEvent);

                    if (entity instanceof Player player) {
                        player.clearActiveItem();
                    }

                    // Advance chain if there are children, otherwise reset
                    if (active.getNode().hasChildren()) {
                        active.getState().advanceTo(active.getNode());
                    } else {
                        active.getState().reset();
                    }
                } else {
                    // Hard timeout - complete as failure
                    InteractionResult.Fail timeoutResult = new InteractionResult.Fail(InteractionResult.FailReason.TIMEOUT);
                    active.getInteraction().then(actor, active.getContext(), timeoutResult,
                            active.getItemInstance(), active.getItemStack());

                    // Execute follow-up interactions
                    executeFollowUps(actor, active.getNode(), active.getContext(), timeoutResult,
                            active.getItemInstance(), active.getItemStack());

                    // Fire timeout event
                    InteractionChainTimeoutEvent timeoutEvent = new InteractionChainTimeoutEvent(
                            actor, active.getChain().getId(), active.getNode(), active.getContext());
                    UtilServer.callEvent(timeoutEvent);

                    // Reset state - chain breaks on timeout
                    active.getState().reset();
                }
                continue;
            }

            // Check interval
            if (currentTick - active.getLastExecutionTick() < active.getIntervalTicks()) {
                continue;
            }

            // Refresh actor in case entity state changed
            InteractionActor actor = createActor(entity);

            // Clear FIRST_RUN since this is not the first execution (first execution happens in executeInteraction)
            active.getContext().remove(InputMeta.FIRST_RUN);

            // Check if this is the last execution before timeout
            // Calculate when the next tick would occur and check if it would timeout
            long nextTickTime = currentTime + (active.getIntervalTicks() * 50L);
            boolean isLastRun = (nextTickTime - active.getStartTime()) > active.getMaxRuntimeMillis();
            if (isLastRun) {
                active.getContext().set(InputMeta.LAST_RUN, true);
            }

            // Execute the interaction
            InteractionResult result = active.getInteraction().execute(
                    actor, active.getContext(), active.getItemInstance(), active.getItemStack());

            // Update last execution tick
            active.setLastExecutionTick(currentTick);

            // Handle result
            if (result.isComplete()) {
                iterator.remove();

                // Call then callback
                active.getInteraction().then(actor, active.getContext(), result,
                        active.getItemInstance(), active.getItemStack());

                // Clear running interaction meta keys after then() so interactions can check them
                active.getContext().remove(InputMeta.LAST_RUN);

                // Execute follow-up interactions
                executeFollowUps(actor, active.getNode(), active.getContext(), result,
                        active.getItemInstance(), active.getItemStack());

                if (result instanceof InteractionResult.Success success) {
                    // Handle item consumption
                    if (active.getInteraction().consumesItem()) {
                        UtilInventory.consumeHand(entity);
                    }

                    // Advance chain if successful
                    if (success.shouldAdvanceChain()) {
                        advanceChain(actor, active.getNode(), active.getState(), active.getChain(), active.getContext());
                    }
                } else if (result instanceof InteractionResult.Fail) {
                    // Failed during execution - reset state
                    active.getState().reset();
                }
            } else if (result instanceof InteractionResult.Running running) {
                // Allow interaction to update interval/runtime/gracefulTimeout dynamically
                active.setIntervalTicks(running.intervalTicks());
                active.setMaxRuntimeMillis(running.maxRuntimeMillis());
                active.setGracefulTimeout(running.gracefulTimeout());
            }
        }
    }

    // MARK: Click Handlers

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack itemStack = event.getPlayer().getEquipment().getItem(event.getHand());
        if (itemStack.getType().isAir()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block != null && (UtilBlock.isInteractable(block) || smartBlockFactory.isSmartBlock(block))) {
            return;
        }

        Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemStack);
        if (itemOpt.isEmpty()) {
            return;
        }

        ItemInstance item = itemOpt.get();
        Player player = event.getPlayer();

        InteractionInput input = switch (event.getAction()) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK ->
                    player.isSneaking() ? InteractionInputs.SHIFT_LEFT_CLICK : InteractionInputs.LEFT_CLICK;
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK ->
                    player.isSneaking() ? InteractionInputs.SHIFT_RIGHT_CLICK : InteractionInputs.RIGHT_CLICK;
            default -> null;
        };

        if (input != null) {
            processInput(player, input, item, itemStack);
        }
    }

    // MARK: Hold Handlers

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(EntityEquipmentChangedEvent event) {
        if (!event.getEquipmentChanges().containsKey(EquipmentSlot.HAND)) {
            return;
        }

        final LivingEntity entity = event.getEntity();
        ItemStack newItem = event.getEquipmentChanges().get(EquipmentSlot.HAND).newItem();
        updateHeldItem(entity, newItem);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItemStack();
        if (event.getSlot() == player.getInventory().getHeldItemSlot()) {
            updateHeldItem(player, newItem);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();
        updateHeldItem(player, itemInMainHand);
    }

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
                processInput(entity, InteractionInputs.HOLD, holdData.getInstance(), holdData.getInstance().getItemStack());
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

        InteractionContainerComponent container = containerOpt.get();
        InteractionChain chain = container.getChain();

        // Check for HOLD_RIGHT_CLICK
        Optional<InteractionChainNode> holdRightClick = chain.findRoot(InteractionInputs.HOLD_RIGHT_CLICK);
        if (holdRightClick.isPresent()) {
            processInput(event.getPlayer(), InteractionInputs.HOLD_RIGHT_CLICK, itemInstance,
                    event.getPlayer().getInventory().getItemInMainHand());
        }
    }

    private void updateHeldItem(LivingEntity livingEntity, @Nullable ItemStack itemStack) {
        heldMap.remove(livingEntity.getUniqueId());

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

        heldMap.put(livingEntity.getUniqueId(), new HoldData(livingEntity, item));
    }

    // MARK: Passive Trigger Handlers

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(DamageEvent event) {
        final LivingEntity damager = event.getDamager();
        if (damager == null) {
            return;
        }


        final EntityEquipment equipment = damager.getEquipment();
        if (equipment == null) {
            return;
        }

        ItemStack itemStack = equipment.getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
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

        // Set context data for damage using InputMeta keys
        InteractionChain chain = containerOpt.get().getChain();
        InteractionState state = stateManager.getOrCreateState(damager.getUniqueId(), chain);
        InteractionContext context = state.getContext();

        context.set(InputMeta.DAMAGE_EVENT, event);
        context.set(InputMeta.DAMAGE_AMOUNT, event.getDamage());
        context.set(InputMeta.FINAL_DAMAGE, event.getModifiedDamage());
        context.set(InputMeta.DAMAGER, damager);
        if (event.isDamageeLiving()) {
            context.set(InputMeta.TARGET, Objects.requireNonNull(event.getLivingDamagee()));
        }

        processInput(damager, InteractionInputs.DAMAGE_DEALT, item, itemStack);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(DamageEvent event) {
        if (!event.isDamageeLiving()) {
            return;
        }

        final LivingEntity damagee = event.getLivingDamagee();
        final EntityEquipment equipment = damagee.getEquipment();
        if (equipment == null) {
            return;
        }

        ItemStack itemStack = equipment.getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
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

        // Set context data for damage taken using InputMeta keys
        InteractionChain chain = containerOpt.get().getChain();
        InteractionState state = stateManager.getOrCreateState(damagee.getUniqueId(), chain);
        InteractionContext context = state.getContext();

        context.set(InputMeta.DAMAGE_EVENT, event);
        context.set(InputMeta.DAMAGE_AMOUNT, event.getDamage());
        context.set(InputMeta.FINAL_DAMAGE, event.getModifiedDamage());
        context.set(InputMeta.TARGET, damagee);
        if (event.getDamager() instanceof LivingEntity damager) {
            context.set(InputMeta.DAMAGER, damager);
        }

        processInput(damagee, InteractionInputs.DAMAGE_TAKEN, item, itemStack);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        ItemStack itemStack = killer.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
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

        // Set context data for kill using InputMeta keys
        InteractionChain chain = containerOpt.get().getChain();
        InteractionState state = stateManager.getOrCreateState(killer.getUniqueId(), chain);
        InteractionContext context = state.getContext();

        context.set(InputMeta.KILLED_ENTITY, event.getEntity());
        context.set(InputMeta.KILLER, killer);

        processInput(killer, InteractionInputs.KILL, item, itemStack);
    }

    @EventHandler
    public void onGamerJoin(ClientJoinEvent event) {
        event.getClient().getGamer().setOffhandExecutor(this::onSwapHand);
    }

    public boolean onSwapHand(@NotNull Client client, @NotNull ItemInstance itemInstance) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
            return false;
        }

        Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(itemStack);
        if (itemOpt.isEmpty()) {
            return false;
        }

        return processInput(player, InteractionInputs.SWAP_HAND, itemOpt.get(), itemStack);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneakToggle(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (itemStack.getType() == Material.AIR) {
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

        // Set context data for sneak using InputMeta keys
        InteractionChain chain = containerOpt.get().getChain();
        InteractionState state = stateManager.getOrCreateState(player.getUniqueId(), chain);
        state.getContext().set(InputMeta.IS_SNEAKING, event.isSneaking());

        InteractionInput input = event.isSneaking() ? InteractionInputs.SNEAK_START : InteractionInputs.SNEAK_END;
        processInput(player, input, item, itemStack);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        heldMap.remove(playerId);
        stateManager.removeActor(playerId);

        // Remove all active interactions for this player
        activeInteractions.entrySet().removeIf(entry -> entry.getKey().actorId().equals(playerId));
    }

    /**
     * Key for tracking active interactions.
     * Unique per player, chain, and interaction name.
     */
    private record ActiveInteractionKey(UUID actorId, long chainId, String interactionName) {}

    /**
     * Tracks a running interaction that needs to be ticked.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    private static class ActiveInteraction {
        final UUID actorId;
        final InteractionActor actor;
        final Interaction interaction;
        final InteractionChainNode node;
        final InteractionState state;
        final InteractionChain chain;
        final InteractionContext context;
        final ItemInstance itemInstance;
        final ItemStack itemStack;
        final InteractionInput input;
        final long startTime;
        long lastExecutionTick;
        int intervalTicks;
        long maxRuntimeMillis;
        boolean gracefulTimeout;
    }

    @Value
    private static class HoldData {
        LivingEntity entity;
        ItemInstance instance;
    }
}
