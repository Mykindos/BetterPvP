package me.mykindos.betterpvp.champions.weapons.impl.runes.insight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class InsightT1 extends InsightBase {

    @Inject
    protected InsightT1(Champions plugin) {
        super(plugin, "insight_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
