package me.mykindos.betterpvp.core.interaction.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.chain.InteractionChainNode;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when an interaction chain has been fully completed.
 * A chain is complete when a leaf node (no children) is successfully executed.
 */
@Getter
public class InteractionChainCompleteEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();

    private final InteractionActor actor;
    private final long chainId;
    private final InteractionChainNode finalNode;
    private final InteractionContext context;

    public InteractionChainCompleteEvent(@NotNull InteractionActor actor, long chainId,
                                          @NotNull InteractionChainNode finalNode, @NotNull InteractionContext context) {
        this.actor = actor;
        this.chainId = chainId;
        this.finalNode = finalNode;
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
