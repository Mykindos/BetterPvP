package me.mykindos.betterpvp.champions.weapons.impl.runes.insight;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class InsightT3 extends InsightBase {

    @Inject
    protected InsightT3(Champions plugin) {
        super(plugin, "insight_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }
}
