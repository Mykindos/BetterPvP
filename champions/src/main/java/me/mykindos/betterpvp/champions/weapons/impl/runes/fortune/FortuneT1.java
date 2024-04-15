package me.mykindos.betterpvp.champions.weapons.impl.runes.fortune;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class FortuneT1 extends FortuneBase {

    @Inject
    protected FortuneT1(Champions plugin) {
        super(plugin, "fortune_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }


}
