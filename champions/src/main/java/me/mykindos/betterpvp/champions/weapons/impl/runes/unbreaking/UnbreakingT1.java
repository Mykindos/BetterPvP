package me.mykindos.betterpvp.champions.weapons.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class UnbreakingT1 extends UnbreakingBase {

    @Inject
    protected UnbreakingT1(Champions plugin) {
        super(plugin, "unbreaking_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
