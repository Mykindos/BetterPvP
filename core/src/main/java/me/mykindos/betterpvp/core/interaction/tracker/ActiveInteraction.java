package me.mykindos.betterpvp.core.interaction.tracker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChain;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.interaction.state.InteractionState;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Tracks a running interaction that needs to be ticked.
 */
@Getter
@Setter
@AllArgsConstructor
public class ActiveInteraction {
    private final UUID actorId;
    private final InteractionActor actor;
    private final Interaction interaction;
    private final InteractionChainNode node;
    private final InteractionState state;
    private final InteractionChain chain;
    private final InteractionContext context;
    private final ItemInstance itemInstance;
    private final ItemStack itemStack;
    private final InteractionInput input;
    private final long executionId;
    private final long startTime;
    private long lastExecutionTick;
    private int intervalTicks;
    private long maxRuntimeMillis;
    private boolean gracefulTimeout;
}
