package me.mykindos.betterpvp.champions.weapons.impl.runes.alacrity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class AlacrityT4 extends AlacrityBase {

    @Inject
    protected AlacrityT4(Champions plugin) {
        super(plugin, "alacrity_rune_t4");
    }

    @Override
    public int getTier() {
        return 4;
    }
}
