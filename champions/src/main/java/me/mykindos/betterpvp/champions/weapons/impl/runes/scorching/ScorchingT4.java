package me.mykindos.betterpvp.champions.weapons.impl.runes.scorching;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ScorchingT4 extends ScorchingBase {

    @Inject
    protected ScorchingT4(Champions plugin) {
        super(plugin, "scorching_rune_t4");
    }

    @Override
    public int getTier() {
        return 3;
    }


}
