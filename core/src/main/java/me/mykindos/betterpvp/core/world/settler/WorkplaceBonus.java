package me.mykindos.betterpvp.core.world.settler;

import me.mykindos.betterpvp.core.world.settler.morale.MoraleEngine;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * What the settlers working at a workplace add to it together. Each brings its own share, made stronger or weaker by
 * its morale, and the total stops at the workplace's cap. Settlers assigned there but not at work, such as strikers,
 * bring nothing.
 */
public final class WorkplaceBonus {

    private WorkplaceBonus() {
    }

    /** The settlers at work at {@code workplace}. */
    public static @NotNull List<Settler> residents(@NotNull Roster roster, @NotNull String workplace) {
        return roster.assignedTo(workplace).stream()
                .filter(settler -> settler.getState() == SettlerState.WORKING)
                .toList();
    }

    /** Every resident's {@code share}, times its morale multiplier, added up and capped at {@code cap}. */
    public static double total(@NotNull List<Settler> residents, @NotNull ToDoubleFunction<Settler> share, double cap) {
        final double total = residents.stream()
                .mapToDouble(settler -> share.applyAsDouble(settler) * MoraleEngine.multiplier(settler))
                .sum();
        return Math.min(cap, Math.max(0, total));
    }
}
