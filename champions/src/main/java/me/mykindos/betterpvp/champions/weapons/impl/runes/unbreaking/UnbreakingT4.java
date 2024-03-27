package me.mykindos.betterpvp.champions.weapons.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class UnbreakingT4 extends UnbreakingBase {

    @Inject
    protected UnbreakingT4(Champions plugin) {
        super(plugin, "unbreaking_rune_t4");
    }

    @Override
    public int getTier() {
        return 4;
    }
}
