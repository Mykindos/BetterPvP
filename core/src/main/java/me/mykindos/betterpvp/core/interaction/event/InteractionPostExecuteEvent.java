package me.mykindos.betterpvp.core.interaction.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.interaction.Interaction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInput;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called after an interaction has been executed.
 * This event is not cancellable.
 */
@Getter
public class InteractionPostExecuteEvent extends CustomEvent {

    private static final HandlerList handlers = new HandlerList();

    private final InteractionActor actor;
    private final Interaction interaction;
    private final InteractionInput input;
    private final InteractionContext context;
    private final InteractionResult result;
    @Nullable
    private final ItemInstance itemInstance;
    @Nullable
    private final ItemStack itemStack;

    public InteractionPostExecuteEvent(@NotNull InteractionActor actor, @NotNull Interaction interaction,
                                        @NotNull InteractionInput input, @NotNull InteractionContext context,
                                        @NotNull InteractionResult result, @Nullable ItemInstance itemInstance,
                                        @Nullable ItemStack itemStack) {
        this.actor = actor;
        this.interaction = interaction;
        this.input = input;
        this.context = context;
        this.result = result;
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
