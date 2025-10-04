package me.mykindos.betterpvp.core.loot;

/**
 * Defines the strategy for awarding loot from a {@link LootBundle}.
 */
public interface AwardStrategy {

    AwardStrategy DEFAULT = bundle -> {
        for (Loot<?, ?> loot : bundle) {
            loot.award(bundle.getContext());
        }
    };

    /**
     * Awards the loot in the bundle.
     * @param bundle The bundle containing the loot to award.
     */
    void award(LootBundle bundle);

}
