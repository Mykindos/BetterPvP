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
import me.mykindos.betterpvp.core.interaction.event.InteractionChainTimeoutEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages interaction states for all actors.
 * Handles state creation, retrieval, and timeout processing.
 */
@CustomLog
@BPvPListener
@Singleton
public class InteractionStateManager implements Listener {

    /**
     * Map of actor UUID -> (chain key -> state).
     */
    private final Map<UUID, Map<Long, InteractionState>> states = new ConcurrentHashMap<>();

    private final ClientManager clientManager;
    private final EnergyService energyService;
    private final EffectManager effectManager;

    @Inject
    public InteractionStateManager(ClientManager clientManager, EnergyService energyService, EffectManager effectManager) {
        this.clientManager = clientManager;
        this.energyService = energyService;
        this.effectManager = effectManager;
    }

    /**
     * Get or create a state for the given actor and chain.
     *
     * @param actorId the actor's unique ID
     * @param chain   the interaction chain
     * @return the state
     */
    @NotNull
    public InteractionState getOrCreateState(@NotNull UUID actorId, @NotNull InteractionChain chain) {
        return states.computeIfAbsent(actorId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(chain.getId(), k -> new InteractionState());
    }

    /**
     * Get the state for the given actor and chain, if it exists.
     *
     * @param actorId the actor's unique ID
     * @param chain   the interaction chain
     * @return the state, or null if none exists
     */
    public InteractionState getState(@NotNull UUID actorId, @NotNull InteractionChain chain) {
        Map<Long, InteractionState> actorStates = states.get(actorId);
        if (actorStates == null) {
            return null;
        }
        return actorStates.get(chain.getId());
    }

    /**
     * Reset the state for the given actor and chain.
     *
     * @param actorId the actor's unique ID
     * @param chain   the interaction chain
     */
    public void resetState(@NotNull UUID actorId, @NotNull InteractionChain chain) {
        Map<Long, InteractionState> actorStates = states.get(actorId);
        if (actorStates != null) {
            InteractionState state = actorStates.get(chain.getId());
            if (state != null) {
                state.reset();
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
    }

    /**
     * Process timeouts for all states.
     * Called periodically via UpdateEvent.
     */
    @UpdateEvent(delay = 100)
    public void tick() {
        Iterator<Map.Entry<UUID, Map<Long, InteractionState>>> actorIterator = states.entrySet().iterator();

        while (actorIterator.hasNext()) {
            Map.Entry<UUID, Map<Long, InteractionState>> actorEntry = actorIterator.next();
            UUID actorId = actorEntry.getKey();
            Map<Long, InteractionState> actorStates = actorEntry.getValue();

            // Check if player is still online
            Player player = Bukkit.getPlayer(actorId);
            if (player == null || !player.isOnline()) {
                actorIterator.remove();
                continue;
            }

            // Process each chain state
            Client client = clientManager.search().online(player);
            InteractionActor actor = new PlayerInteractionActor(player, client, energyService, effectManager);

            actorStates.forEach((chainId, state) -> {
                if (state.isInChain() && state.hasTimedOut()) {
                    // Fire timeout event
                    InteractionChainTimeoutEvent event = new InteractionChainTimeoutEvent(
                            actor,
                            chainId,
                            state.getCurrentNode(),
                            state.getContext()
                    );
                    UtilServer.callEvent(event);

                    // Reset the state
                    state.reset();
                }
            });

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
