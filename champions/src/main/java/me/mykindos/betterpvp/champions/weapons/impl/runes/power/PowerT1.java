package me.mykindos.betterpvp.champions.weapons.impl.runes.power;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class PowerT1 extends PowerBase {

    @Inject
    protected PowerT1(Champions plugin) {
        super(plugin, "power_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
