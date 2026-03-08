package me.mykindos.betterpvp.core.interaction.tracker;

import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.item.ItemInstance;

import java.util.UUID;

/**
 * Tracks an active HOLD_RIGHT_CLICK execution lifecycle.
 */
@Getter
public class ActiveHold {
    private final UUID actorId;
    private final InteractionChainNode node;
    private final InteractionState state;
    private final InteractionChain chain;
    private final ItemInstance itemInstance;
    private final long executionId;
    private final long startTime;

    public ActiveHold(UUID actorId, InteractionChainNode node, InteractionState state,
                      InteractionChain chain, ItemInstance itemInstance, long executionId) {
        this.actorId = actorId;
        this.node = node;
        this.state = state;
        this.chain = chain;
        this.itemInstance = itemInstance;
        this.executionId = executionId;
        this.startTime = System.currentTimeMillis();
    }
}
