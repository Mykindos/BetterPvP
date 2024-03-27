package me.mykindos.betterpvp.champions.weapons.impl.runes.conquering;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ConqueringT1 extends ConqueringBase {

    @Inject
    protected ConqueringT1(Champions plugin) {
        super(plugin, "conquering_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
