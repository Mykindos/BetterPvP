package me.mykindos.betterpvp.champions.weapons.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class UnbreakingT2 extends UnbreakingBase {

    @Inject
    protected UnbreakingT2(Champions plugin) {
        super(plugin, "unbreaking_rune_t2");
    }

    @Override
    public int getTier() {
        return 2;
    }
}
