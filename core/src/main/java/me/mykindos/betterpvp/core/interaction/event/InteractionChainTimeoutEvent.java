package me.mykindos.betterpvp.core.interaction.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when an interaction chain times out.
 * This happens when the actor doesn't provide the next input within the timeout period.
 */
@Getter
public class InteractionChainTimeoutEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();

    private final InteractionActor actor;
    private final long chainId;
    @Nullable
    private final InteractionChainNode lastNode;
    private final InteractionContext context;

    public InteractionChainTimeoutEvent(@NotNull InteractionActor actor, long chainId,
                                         @Nullable InteractionChainNode lastNode, @NotNull InteractionContext context) {
        this.actor = actor;
        this.chainId = chainId;
        this.lastNode = lastNode;
        this.context = context;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
