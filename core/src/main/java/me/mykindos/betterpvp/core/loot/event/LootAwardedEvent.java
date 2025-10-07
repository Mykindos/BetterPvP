package me.mykindos.betterpvp.core.loot.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.loot.Loot;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;

/**
 * Called when loot is awarded from a {@link LootBundle}.
 * This event is fired for each individual {@link Loot} item awarded.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LootAwardedEvent extends CustomEvent {

    private final LootBundle bundle;
    private final LootContext context;
    private final Loot<?, ?> loot;

    public LootAwardedEvent(LootBundle bundle, LootContext context, Loot<?, ?> loot) {
        this.bundle = bundle;
        this.context = context;
        this.loot = loot;
    }

}
