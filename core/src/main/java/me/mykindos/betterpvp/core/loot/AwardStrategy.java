package me.mykindos.betterpvp.core.loot;

import me.mykindos.betterpvp.core.loot.event.LootAwardedEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;

/**
 * Defines the strategy for awarding loot from a {@link LootBundle}.
 */
public interface AwardStrategy {

    AwardStrategy DEFAULT = bundle -> {
        for (Loot<?, ?> loot : bundle) {
            loot.award(bundle.getContext());
            UtilServer.callEvent(new LootAwardedEvent(bundle, bundle.getContext(), loot));
        }
    };

    /**
     * Awards the loot in the bundle.
     * @param bundle The bundle containing the loot to award.
     */
    void award(LootBundle bundle);

}
