package me.mykindos.betterpvp.champions.weapons.impl.runes.frost;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class FrostT4 extends FrostBase {

    @Inject
    protected FrostT4(Champions plugin) {
        super(plugin, "frost_rune_t4");
    }

    @Override
    public int getTier() {
        return 3;
    }


}
