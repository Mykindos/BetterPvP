package me.mykindos.betterpvp.core.interaction.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyService;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.actor.PlayerInteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.event.InteractionChainTimeoutEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manages interaction states for all actors.
 * Handles state creation, retrieval, and timeout processing.
 * <p>
 * States are keyed by (actorId, chainId, executionId) to ensure complete isolation
 * between different chain executions. Each root invocation gets a unique executionId,
 * preventing state from bleeding between parallel executions of the same root input.
 */
@CustomLog
@BPvPListener
@Singleton
public class InteractionStateManager implements Listener {

    /**
     * Key for identifying a unique state per actor, chain, and execution.
     */
    public record StateKey(long chainId, long executionId) {}

    /**
     * Map of actor UUID -> (StateKey -> state).
     */
    private final Map<UUID, Map<StateKey, InteractionState>> states = new ConcurrentHashMap<>();

    /**
     * Map of actor UUID -> (rootNodeId -> last chain completion time).
     * Used to track cooldowns for root nodes, which only start counting after the chain completes.
     * Keyed by root node ID to ensure each root has its own independent cooldown.
     */
    private final Map<UUID, Map<Long, Long>> rootCooldowns = new ConcurrentHashMap<>();

    /**
     * Counter for generating unique execution IDs.
     */
    private final AtomicLong executionIdCounter = new AtomicLong(0);

    private final ClientManager clientManager;
    private final EnergyService energyService;
    private final EffectManager effectManager;

    @Inject
    public InteractionStateManager(ClientManager clientManager, EnergyService energyService,
                                   EffectManager effectManager) {
        this.clientManager = clientManager;
        this.energyService = energyService;
        this.effectManager = effectManager;
    }

    /**
     * Generate a new unique execution ID.
     *
     * @return a new execution ID
     */
    public long nextExecutionId() {
        return executionIdCounter.incrementAndGet();
    }

    /**
     * Create a new state for the given actor, chain, and execution.
     * Always creates a new state - use this when starting a new root execution.
     *
     * @param actorId     the actor's unique ID
     * @param chain       the interaction chain
     * @param executionId the unique execution ID for this chain invocation
     * @return the newly created state
     */
    @NotNull
    public InteractionState createState(@NotNull UUID actorId, @NotNull InteractionChain chain, long executionId) {
        InteractionState state = new InteractionState();
        states.computeIfAbsent(actorId, k -> new ConcurrentHashMap<>())
                .put(new StateKey(chain.getId(), executionId), state);
        return state;
    }

    /**
     * Get an existing state for the given actor, chain, and execution ID.
     *
     * @param actorId     the actor's unique ID
     * @param chain       the interaction chain
     * @param executionId the execution ID
     * @return the state, or null if none exists
     */
    @Nullable
    public InteractionState getState(@NotNull UUID actorId, @NotNull InteractionChain chain, long executionId) {
        Map<StateKey, InteractionState> actorStates = states.get(actorId);
        if (actorStates == null) {
            return null;
        }
        return actorStates.get(new StateKey(chain.getId(), executionId));
    }

    /**
     * Find an active chain for the given actor and chain.
     * Searches all execution states and returns the first one that is currently in a chain.
     *
     * @param actorId the actor's unique ID
     * @param chain   the interaction chain
     * @return info about the active chain, or empty if no chain is active
     */
    public Optional<ActiveChainInfo> findActiveChain(@NotNull UUID actorId, @NotNull InteractionChain chain) {
        Map<StateKey, InteractionState> actorStates = states.get(actorId);
        if (actorStates == null) {
            return Optional.empty();
        }

        long chainId = chain.getId();
        for (Map.Entry<StateKey, InteractionState> entry : actorStates.entrySet()) {
            StateKey key = entry.getKey();
            InteractionState state = entry.getValue();
            if (key.chainId() == chainId && state.isInChain()) {
                return Optional.of(new ActiveChainInfo(key.executionId(), state));
            }
        }
        return Optional.empty();
    }

    /**
     * Information about an active chain.
     */
    public record ActiveChainInfo(long executionId, InteractionState state) {}

    /**
     * Find an active chain for the given actor, chain, and root node.
     * Searches all execution states and returns the one that was started by the specified root.
     *
     * @param actorId    the actor's unique ID
     * @param chain      the interaction chain
     * @param rootNodeId the root node ID to match
     * @return info about the active chain with that root, or empty if none exists
     */
    public Optional<ActiveChainInfo> findActiveChainByRoot(@NotNull UUID actorId, @NotNull InteractionChain chain, long rootNodeId) {
        Map<StateKey, InteractionState> actorStates = states.get(actorId);
        if (actorStates == null) {
            return Optional.empty();
        }

        long chainId = chain.getId();
        for (Map.Entry<StateKey, InteractionState> entry : actorStates.entrySet()) {
            StateKey key = entry.getKey();
            InteractionState state = entry.getValue();
            InteractionChainNode rootNode = state.getRootNode();
            if (key.chainId() == chainId && state.isInChain() && rootNode != null && rootNode.getId() == rootNodeId) {
                return Optional.of(new ActiveChainInfo(key.executionId(), state));
            }
        }
        return Optional.empty();
    }

    /**
     * Remove a specific execution state.
     *
     * @param actorId     the actor's unique ID
     * @param chain       the interaction chain
     * @param executionId the execution ID to remove
     */
    public void removeState(@NotNull UUID actorId, @NotNull InteractionChain chain, long executionId) {
        Map<StateKey, InteractionState> actorStates = states.get(actorId);
        if (actorStates != null) {
            actorStates.remove(new StateKey(chain.getId(), executionId));
            if (actorStates.isEmpty()) {
                states.remove(actorId);
            }
        }
    }

    /**
     * Remove all states for the given actor.
     *
     * @param actorId the actor's unique ID
     */
    public void removeActor(@NotNull UUID actorId) {
        states.remove(actorId);
        rootCooldowns.remove(actorId);
    }

    /**
     * Record that a chain has completed (either success or fail).
     * This is used to start the cooldown timer for the specific root node.
     *
     * @param actorId    the actor's unique ID
     * @param rootNodeId the root node's unique ID
     */
    public void recordChainCompletion(@NotNull UUID actorId, long rootNodeId) {
        rootCooldowns.computeIfAbsent(actorId, k -> new ConcurrentHashMap<>())
                .put(rootNodeId, System.currentTimeMillis());
    }

    /**
     * Check if the root cooldown has passed for the given root node.
     * Root cooldowns are measured from the last chain completion time, not from the root execution time.
     *
     * @param actorId            the actor's unique ID
     * @param rootNodeId         the root node's unique ID
     * @param minimumDelayMillis the minimum delay required in milliseconds
     * @return true if the cooldown has passed or if no previous completion exists
     */
    public boolean hasRootCooldownPassed(@NotNull UUID actorId, long rootNodeId, long minimumDelayMillis) {
        if (minimumDelayMillis <= 0) {
            return true;
        }

        Map<Long, Long> actorCooldowns = rootCooldowns.get(actorId);
        if (actorCooldowns == null) {
            return true; // No previous completion, allow execution
        }

        Long lastCompletion = actorCooldowns.get(rootNodeId);
        if (lastCompletion == null) {
            return true; // No previous completion for this root
        }

        return System.currentTimeMillis() - lastCompletion >= minimumDelayMillis;
    }

    /**
     * Process timeouts for all states.
     * Called periodically via UpdateEvent.
     */
    @UpdateEvent(delay = 50)
    public void tick() {
        Iterator<Map.Entry<UUID, Map<StateKey, InteractionState>>> actorIterator = states.entrySet().iterator();

        while (actorIterator.hasNext()) {
            Map.Entry<UUID, Map<StateKey, InteractionState>> actorEntry = actorIterator.next();
            UUID actorId = actorEntry.getKey();
            Map<StateKey, InteractionState> actorStates = actorEntry.getValue();

            // Check if player is still online
            Player player = Bukkit.getPlayer(actorId);
            if (player == null || !player.isOnline()) {
                actorIterator.remove();
                continue;
            }

            // Process each chain state
            Client client = clientManager.search().online(player);
            InteractionActor actor = new PlayerInteractionActor(player, client, energyService, effectManager);

            // Use iterator to allow removal during iteration
            Iterator<Map.Entry<StateKey, InteractionState>> stateIterator = actorStates.entrySet().iterator();
            while (stateIterator.hasNext()) {
                Map.Entry<StateKey, InteractionState> stateEntry = stateIterator.next();
                StateKey stateKey = stateEntry.getKey();
                InteractionState state = stateEntry.getValue();

                if (state.isInChain() && state.hasTimedOut()) {
                    // Record chain completion for root cooldown tracking before removing
                    InteractionChainNode rootNode = state.getRootNode();
                    if (rootNode != null) {
                        recordChainCompletion(actorId, rootNode.getId());
                    }

                    // Fire timeout event
                    InteractionChainTimeoutEvent event = new InteractionChainTimeoutEvent(
                            actor,
                            stateKey.chainId(),
                            state.getCurrentNode(),
                            state.getContext()
                    );
                    UtilServer.callEvent(event);

                    // Remove the timed out state entirely
                    stateIterator.remove();
                } else if (!state.isInChain()) {
                    // Clean up states that are no longer in a chain
                    stateIterator.remove();
                }
            }

            // Clean up empty state maps
            if (actorStates.isEmpty()) {
                actorIterator.remove();
            }
        }
    }

    /**
     * Get the number of tracked actors.
     *
     * @return the number of actors
     */
    public int getTrackedActorCount() {
        return states.size();
    }

    /**
     * Clear all states. Used for cleanup/reload.
     */
    public void clear() {
        states.clear();
    }
}
