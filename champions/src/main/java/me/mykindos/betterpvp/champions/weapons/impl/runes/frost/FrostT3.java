package me.mykindos.betterpvp.champions.weapons.impl.runes.frost;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;

@Singleton
public class FrostT3 extends FrostBase {

    @Inject
    protected FrostT3(Champions plugin) {
        super(plugin, "frost_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }


}
