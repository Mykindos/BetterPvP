package me.mykindos.betterpvp.champions.weapons.impl.runes.reinforced;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ReinforcingT2 extends ReinforcingBase {

    @Inject
    protected ReinforcingT2(Champions plugin) {
        super(plugin, "reinforcing_rune_t2");
    }

    @Override
    public int getTier() {
        return 2;
    }
}
