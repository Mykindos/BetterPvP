package me.mykindos.betterpvp.clans.world.camp.storage;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

/**
 * Where items put into a set of chests go, worked out on kinds and counts alone. Each item first tops up stacks of the
 * same kind, then takes empty slots in chests already holding its kind, then empty slots in any chest. Whatever finds
 * no room is left out of the plan.
 */
public final class SortingPlan {

    private SortingPlan() {
    }

    /** Some amount of one kind of item. */
    @Value
    public static class Pile<K> {
        K kind;
        int amount;
    }

    /** {@code amount} of item {@code item} goes into slot {@code slot} of chest {@code chest}. */
    @Value
    public static class Move {
        int item;
        int chest;
        int slot;
        int amount;
    }

    /**
     * @param chests each chest's slots in order, null for an empty slot
     * @param items  what is being put in, in the order it is taken
     * @param same   whether two kinds stack together
     * @param max    how many of a kind fit in one slot
     */
    public static <K> @NotNull List<Move> plan(@NotNull List<List<Pile<K>>> chests,
                                               @NotNull List<Pile<K>> items, @NotNull BiPredicate<K, K> same,
                                               @NotNull ToIntFunction<K> max) {
        final List<List<Pile<K>>> state = new ArrayList<>();
        chests.forEach(chest -> state.add(new ArrayList<>(chest)));
        final List<Move> moves = new ArrayList<>();

        for (int item = 0; item < items.size(); item++) {
            final Pile<K> pile = items.get(item);
            final K kind = pile.getKind();
            final int limit = max.applyAsInt(kind);
            int left = pile.getAmount();

            final List<Integer> holding = new ArrayList<>();
            for (int chest = 0; chest < state.size(); chest++) {
                if (state.get(chest).stream().anyMatch(slot -> slot != null && same.test(slot.getKind(), kind))) {
                    holding.add(chest);
                }
            }

            for (int chest : holding) {
                final List<Pile<K>> slots = state.get(chest);
                for (int slot = 0; slot < slots.size() && left > 0; slot++) {
                    final Pile<K> there = slots.get(slot);
                    if (there != null && same.test(there.getKind(), kind) && there.getAmount() < limit) {
                        final int amount = Math.min(left, limit - there.getAmount());
                        slots.set(slot, new Pile<>(there.getKind(), there.getAmount() + amount));
                        moves.add(new Move(item, chest, slot, amount));
                        left -= amount;
                    }
                }
            }
            for (int chest : holding) {
                left = fillEmpty(state, moves, item, chest, kind, limit, left);
            }
            for (int chest = 0; chest < state.size() && left > 0; chest++) {
                left = fillEmpty(state, moves, item, chest, kind, limit, left);
            }
        }
        return moves;
    }

    private static <K> int fillEmpty(@NotNull List<List<Pile<K>>> state, @NotNull List<Move> moves, int item,
                                     int chest, K kind, int limit, int left) {
        final List<Pile<K>> slots = state.get(chest);
        for (int slot = 0; slot < slots.size() && left > 0; slot++) {
            if (slots.get(slot) == null) {
                final int amount = Math.min(left, limit);
                slots.set(slot, new Pile<>(kind, amount));
                moves.add(new Move(item, chest, slot, amount));
                left -= amount;
            }
        }
        return left;
    }
}
