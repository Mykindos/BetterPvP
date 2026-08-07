package me.mykindos.betterpvp.core.interaction;

import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.temper.TemperService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interaction paid for by the item running it rather than by its wielder, spending the item's
 * temper for as long as it is held.
 * <p>
 * The bar is checked against the item's activation minimum only on the first step, so a channel is
 * never cut the instant it drops below that threshold — it runs until the bar is actually spent.
 * That is what makes an item able to demand a full bar to start while still being usable down to
 * empty once started.
 *
 * @see me.mykindos.betterpvp.core.item.temper.TemperProfile
 */
@Getter
public abstract class TemperInteraction extends AbstractInteraction {

    protected final TemperService temperService;

    protected TemperInteraction(@NotNull String name, @NotNull TemperService temperService) {
        super(name);
        this.temperService = temperService;
    }

    /**
     * Scales the item's drain rate for this step, for abilities that cost more in some states than
     * in others.
     *
     * @return a multiplier on the profile's drain rate, 1 for the plain rate
     */
    protected double getDrainMultiplier(@NotNull InteractionActor actor, @NotNull InteractionContext context) {
        return 1;
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                   @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
        if (!(actor.getEntity() instanceof Player player) || itemInstance == null) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
        }

        final boolean starting = context.has(InputMeta.FIRST_RUN);
        if (temperService.isSpent(player, itemInstance) || (starting && !temperService.canActivate(player, itemInstance))) {
            return new InteractionResult.Fail(InteractionResult.FailReason.TEMPER);
        }

        // Whether the ability may run at all is the subclass's call, so it gets asked before any
        // temper is spent — an ability that refuses this step should not be charged for it.
        final InteractionResult result = doTemperExecute(actor, context, itemInstance, itemStack);
        if (result.isSuccess()) {
            temperService.drain(player, itemInstance, getDrainMultiplier(actor, context));
        }
        return result;
    }

    /**
     * Perform the actual interaction execution. Temper is only spent if this succeeds.
     *
     * @param actor        the actor performing the interaction
     * @param context      the interaction context
     * @param itemInstance the item instance running the ability
     * @param itemStack    the item stack (may be null)
     * @return the result of the interaction
     */
    @NotNull
    protected abstract InteractionResult doTemperExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                          @NotNull ItemInstance itemInstance, @Nullable ItemStack itemStack);
}
