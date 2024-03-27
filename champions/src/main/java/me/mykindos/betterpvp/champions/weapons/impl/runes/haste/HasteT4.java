package me.mykindos.betterpvp.champions.weapons.impl.runes.haste;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class HasteT4 extends HasteBase {

    @Inject
    protected HasteT4(Champions plugin) {
        super(plugin, "haste_rune_t4");
    }

    @Override
    public int getTier() {
        return 4;
    }
}
