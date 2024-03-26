package me.mykindos.betterpvp.champions.weapons.impl.runes.conquering;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ConqueringT2 extends ConqueringBase {

    @Inject
    protected ConqueringT2(Champions plugin) {
        super(plugin, "conquering_rune_t2");
    }

    @Override
    public int getTier() {
        return 2;
    }
}
