package me.mykindos.betterpvp.champions.weapons.impl.runes.power;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class PowerT2 extends PowerBase {

    @Inject
    protected PowerT2(Champions plugin) {
        super(plugin, "power_rune_t2");
    }

    @Override
    public int getTier() {
        return 2;
    }
}
