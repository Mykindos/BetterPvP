package me.mykindos.betterpvp.champions.weapons.impl.runes.fortune;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class FortuneT2 extends FortuneBase {

    @Inject
    protected FortuneT2(Champions plugin) {
        super(plugin, "fortune_rune_t2");
    }

    @Override
    public int getTier() {
        return 1;
    }


}
