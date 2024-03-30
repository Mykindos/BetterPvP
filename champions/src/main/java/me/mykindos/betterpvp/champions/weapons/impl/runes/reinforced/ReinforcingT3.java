package me.mykindos.betterpvp.champions.weapons.impl.runes.reinforced;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ReinforcingT3 extends ReinforcingBase {

    @Inject
    protected ReinforcingT3(Champions plugin) {
        super(plugin, "reinforcing_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }
}
