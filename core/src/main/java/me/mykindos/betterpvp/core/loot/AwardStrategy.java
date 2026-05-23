package me.mykindos.betterpvp.core.loot;

import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.Nullable;

/**
 * Defines the strategy for awarding loot from a {@link LootBundle}.
 * <p>
 * This is an abstract class — not an interface — so that the per-loot award path is owned
 * by the framework. Concrete strategies invoke {@link #awardSingle(LootBundle, Loot)} to
 * trigger {@link Loot#award(LootContext)} for a single entry; that helper is the ONLY way
 * to award a loot, and it always fires a {@link LootAwardedEvent} with the result. There
 * is intentionally no way for a strategy implementation to award loot without firing the
 * event.
 * <p>
 * Use the {@link #DEFAULT} singleton for "award every entry immediately". Strategies that
 * need custom ordering, scheduling, or per-item processing subclass this and call
 * {@code awardSingle} from their own {@link #award(LootBundle)} implementation. Strategies
 * that only need to observe awards (set velocity, send a message, increment a counter)
 * should NOT subclass — they should listen to {@link LootAwardedEvent} instead.
 */
public abstract class AwardStrategy {

    public static final AwardStrategy DEFAULT = new AwardStrategy() {
        @Override
        public void award(LootBundle bundle) {
            for (Loot<?, ?> loot : bundle) {
                awardSingle(bundle, loot);
            }
        }
    };

    /**
     * Awards the loot in the bundle.
     * @param bundle The bundle containing the loot to award.
     */
    public abstract void award(LootBundle bundle);

    /**
     * Awards a single {@link Loot} entry and fires the {@link LootAwardedEvent} carrying
     * its result. This is the only callable path to {@link Loot#award(LootContext)}.
     *
     * @param bundle the bundle currently being awarded
     * @param loot   the loot entry to award
     * @param <R>    the loot's reward type
     * @return the raw result produced by {@code loot.award(context)}, or {@code null}
     */
    protected final <T, R> @Nullable R awardSingle(LootBundle bundle, Loot<T, R> loot) {
        final LootContext context = bundle.getContext();
        final R result = loot.award(context);
        UtilServer.callEvent(new LootAwardedEvent(bundle, context, loot, result));
        return result;
    }
}
