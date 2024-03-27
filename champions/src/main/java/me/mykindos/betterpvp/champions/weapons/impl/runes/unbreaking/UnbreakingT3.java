package me.mykindos.betterpvp.champions.weapons.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class UnbreakingT3 extends UnbreakingBase {

    @Inject
    protected UnbreakingT3(Champions plugin) {
        super(plugin, "unbreaking_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }
}
