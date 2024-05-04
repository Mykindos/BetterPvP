package me.mykindos.betterpvp.champions.weapons.impl.runes.scorching;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ScorchingT3 extends ScorchingBase {

    @Inject
    protected ScorchingT3(Champions plugin) {
        super(plugin, "scorching_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }


}
