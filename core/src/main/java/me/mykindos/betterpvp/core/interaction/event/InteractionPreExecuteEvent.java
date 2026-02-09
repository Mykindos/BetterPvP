package me.mykindos.betterpvp.core.interaction.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called before an interaction is executed.
 * Can be cancelled to prevent the interaction from running.
 */
@Getter
public class InteractionPreExecuteEvent extends CustomCancellableEvent {

    private static final HandlerList handlers = new HandlerList();

    private final InteractionActor actor;
    private final Interaction interaction;
    private final InteractionInput input;
    private final InteractionContext context;
    @Nullable
    private final ItemInstance itemInstance;
    @Nullable
    private final ItemStack itemStack;

    public InteractionPreExecuteEvent(@NotNull InteractionActor actor, @NotNull Interaction interaction,
                                       @NotNull InteractionInput input, @NotNull InteractionContext context,
                                       @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        this.actor = actor;
        this.interaction = interaction;
        this.input = input;
        this.context = context;
        this.itemInstance = itemInstance;
        this.itemStack = itemStack;
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
