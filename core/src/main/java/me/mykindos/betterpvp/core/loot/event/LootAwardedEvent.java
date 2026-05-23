package me.mykindos.betterpvp.core.loot.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import org.jetbrains.annotations.Nullable;

/**
 * Called when loot is awarded from a {@link LootBundle}.
 * This event is fired for each individual {@link Loot} item awarded.
 */
@EqualsAndHashCode(callSuper = true)
@Getter
public class LootAwardedEvent extends CustomEvent {

    private final LootBundle bundle;
    private final LootContext context;
    private final Loot<?, ?> loot;
    private final @Nullable Object rawResult;

    public LootAwardedEvent(LootBundle bundle, LootContext context, Loot<?, ?> loot, @Nullable Object rawResult) {
        this.bundle = bundle;
        this.context = context;
        this.loot = loot;
        this.rawResult = rawResult;
    }

    /**
     * Returns the value produced by {@link Loot#award(LootContext)} cast to the caller-expected type.
     * <p>
     * The cast is unchecked — callers must know the awarded reward type for the specific {@link Loot}
     * subtype they are reacting to (e.g. {@code Item} for {@code DroppedItemLoot}, {@code Entity}
     * for {@code EntitySpawnLoot}). A wrong inferred type fails at the use site with ClassCastException.
     *
     * @param <R> the expected reward type
     * @return the awarded result, or {@code null} if the loot's award produced no value
     */
    @SuppressWarnings("unchecked")
    public <R> @Nullable R getResult() {
        return (R) rawResult;
    }
}
