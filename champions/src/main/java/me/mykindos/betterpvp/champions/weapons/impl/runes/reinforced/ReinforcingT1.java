package me.mykindos.betterpvp.champions.weapons.impl.runes.reinforced;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ReinforcingT1 extends ReinforcingBase {

    @Inject
    protected ReinforcingT1(Champions plugin) {
        super(plugin, "reinforcing_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
