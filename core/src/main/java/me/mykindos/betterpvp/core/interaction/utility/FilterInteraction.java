package me.mykindos.betterpvp.core.interaction.utility;

import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.context.InputMeta;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * A utility interaction that filters based on a predicate.
 * Returns {@link InteractionResult.Success#ADVANCE} if the predicate passes,
 * otherwise returns a fail result with {@link InteractionResult.FailReason#CONDITIONS}.
 */
public class FilterInteraction extends AbstractInteraction {

    private final Predicate<InteractionContext> predicate;

    private FilterInteraction(Predicate<InteractionContext> predicate) {
        super("filter");
        this.predicate = predicate;
    }

    /**
     * Create a new FilterInteraction with the given predicate.
     *
     * @param predicate the predicate to test
     * @return a new FilterInteraction
     */
    public static FilterInteraction of(Predicate<InteractionContext> predicate) {
        return new FilterInteraction(predicate);
    }

    /**
     * Create a filter that only passes for melee damage events.
     *
     * @return a filter for melee damage
     */
    public static FilterInteraction meleeDamage() {
        return of(ctx -> ctx.get(InputMeta.DAMAGE_EVENT)
                .map(e -> e.getCause().getCategories().contains(DamageCauseCategory.MELEE))
                .orElse(false));
    }

    @Override
    protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor,
                                                    @NotNull InteractionContext context,
                                                    @Nullable ItemInstance itemInstance,
                                                    @Nullable ItemStack itemStack) {
        return predicate.test(context)
                ? InteractionResult.Success.ADVANCE
                : new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS);
    }
}
