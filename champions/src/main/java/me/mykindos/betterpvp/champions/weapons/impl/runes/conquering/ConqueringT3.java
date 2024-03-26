package me.mykindos.betterpvp.champions.weapons.impl.runes.conquering;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class ConqueringT3 extends ConqueringBase {

    @Inject
    protected ConqueringT3(Champions plugin) {
        super(plugin, "conquering_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }
}
