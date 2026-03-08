package me.mykindos.betterpvp.core.interaction.executor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.EntityInteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.PlayerInteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteraction;
import me.mykindos.betterpvp.core.interaction.tracker.ActiveInteractionKey;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Core execution facade for the interaction system.
 * Routes inputs through the state machine via InputRouter, executes interactions via ExecutionPipeline,
 * and handles results via ResultHandler.
 */
@CustomLog
@Singleton
public class InteractionExecutor {

    private final ClientManager clientManager;
    private final EnergyService energyService;
    private final EffectManager effectManager;
    private final InputRouter router;
    private final ExecutionPipeline pipeline;
    private final ResultHandler resultHandler;

    @Inject
    public InteractionExecutor(ClientManager clientManager, EnergyService energyService,
                               EffectManager effectManager, InputRouter router,
                               ExecutionPipeline pipeline, ResultHandler resultHandler) {
        this.clientManager = clientManager;
        this.energyService = energyService;
        this.effectManager = effectManager;
        this.router = router;
        this.pipeline = pipeline;
        this.resultHandler = resultHandler;
    }

    /**
     * Create an appropriate InteractionActor for the given entity.
     *
     * @param entity the living entity
     * @return a PlayerInteractionActor for players, EntityInteractionActor otherwise
     */
    public InteractionActor createActor(@NotNull LivingEntity entity) {
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
     * @param activeInteractions the map of active interactions (for Running result tracking)
     * @return true if an interaction was triggered
     */
    public boolean processInput(@NotNull LivingEntity livingEntity, @NotNull InteractionInput input,
                                @NotNull ItemInstance itemInstance, @NotNull ItemStack itemStack,
                                @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions) {
        return processInput(livingEntity, input, itemInstance, itemStack, activeInteractions, null);
    }

    /**
     * Process an input for a living entity with an item, with optional execution data setup.
     *
     * @param livingEntity       the living entity
     * @param input              the input type
     * @param itemInstance       the item instance
     * @param itemStack          the item stack
     * @param activeInteractions the map of active interactions (for Running result tracking)
     * @param executionDataSetup optional callback to set execution-scoped data after startExecution()
     * @return true if an interaction was triggered
     */
    public boolean processInput(@NotNull LivingEntity livingEntity, @NotNull InteractionInput input,
                                @NotNull ItemInstance itemInstance, @NotNull ItemStack itemStack,
                                @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions,
                                @Nullable Consumer<InteractionContext> executionDataSetup) {
        Optional<InteractionContainerComponent> containerOpt = itemInstance.getComponent(InteractionContainerComponent.class);
        if (containerOpt.isEmpty()) {
            return false;
        }

        InteractionChain chain = containerOpt.get().getChain();
        UUID actorId = livingEntity.getUniqueId();
        InteractionActor actor = createActor(livingEntity);

        // Route the input
        InputRouter.RouteResult routeResult = router.route(actorId, chain, input);

        return switch (routeResult) {
            case InputRouter.RouteResult.NoMatch() -> false;
            case InputRouter.RouteResult.Consumed() -> true;
            case InputRouter.RouteResult.TargetFound(var node, var state, var execId, var isNew) ->
                    executeTarget(livingEntity, actor, node, state, chain, itemInstance, itemStack,
                            input, execId, isNew, activeInteractions, executionDataSetup);
            case InputRouter.RouteResult.MultipleTargets(var nodes, var aid, var ch) ->
                    executeMultipleTargets(livingEntity, actor, nodes, ch, itemInstance, itemStack,
                            input, activeInteractions, executionDataSetup);
        };
    }

    /**
     * Execute multiple root nodes (for inputs like PASSIVE that can have multiple handlers).
     */
    private boolean executeMultipleTargets(@NotNull LivingEntity entity, @NotNull InteractionActor actor,
                                            @NotNull java.util.List<InteractionChainNode> nodes,
                                            @NotNull InteractionChain chain, @NotNull ItemInstance itemInstance,
                                            @NotNull ItemStack itemStack, @NotNull InteractionInput input,
                                            @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions,
                                            @Nullable Consumer<InteractionContext> executionDataSetup) {
        boolean anyTriggered = false;
        for (InteractionChainNode node : nodes) {
            // Create a new chain state for each root
            long executionId = router.getStateManager().nextExecutionId();
            InteractionState state = router.getStateManager().createState(entity.getUniqueId(), chain, executionId);
            state.setRootNode(node);

            if (executeTarget(entity, actor, node, state, chain, itemInstance, itemStack,
                    input, executionId, true, activeInteractions, executionDataSetup)) {
                anyTriggered = true;
            }
        }
        return anyTriggered;
    }

    /**
     * Execute an interaction for a routed target.
     */
    private boolean executeTarget(@NotNull LivingEntity entity, @NotNull InteractionActor actor,
                                  @NotNull InteractionChainNode node, @NotNull InteractionState state,
                                  @NotNull InteractionChain chain, @NotNull ItemInstance itemInstance,
                                  @NotNull ItemStack itemStack, @NotNull InteractionInput input,
                                  long executionId, boolean isNewChain,
                                  @NotNull Map<ActiveInteractionKey, ActiveInteraction> activeInteractions,
                                  @Nullable Consumer<InteractionContext> executionDataSetup) {
        // Check if already running
        ActiveInteractionKey key = new ActiveInteractionKey(entity.getUniqueId(), node);
        if (activeInteractions.containsKey(key)) {
            return false;
        }

        // Check root cooldown for new chains
        if (isNewChain && node.isRoot()) {
            InteractionContext context = state.getContext();
            if (!router.hasRootCooldownPassed(entity.getUniqueId(), node, context)) {
                router.removeState(entity.getUniqueId(), chain, executionId);
                return true; // Input consumed but not processed - still on cooldown
            }
        }

        // Execute
        ExecutionRequest request = ExecutionRequest.of(entity, actor, node, state, chain,
                itemInstance, itemStack, input, executionId, isNewChain, executionDataSetup);

        Optional<InteractionResult> result = pipeline.execute(request);
        if (result.isEmpty()) {
            return true; // Delay/multi-input consumed
        }

        // Handle result
        ResultContext ctx = new ResultContext(entity, actor, node.getInteraction(), node, state, chain,
                state.getContext(), result.get(), itemInstance, itemStack, input, executionId, activeInteractions);
        return resultHandler.handle(ctx);
    }

    /**
     * Get the result handler for external use.
     *
     * @return the result handler
     */
    public ResultHandler getResultHandler() {
        return resultHandler;
    }

    /**
     * Get the input router for external use.
     *
     * @return the input router
     */
    public InputRouter getRouter() {
        return router;
    }

    /**
     * Get the execution pipeline for external use.
     *
     * @return the execution pipeline
     */
    public ExecutionPipeline getPipeline() {
        return pipeline;
    }

}
