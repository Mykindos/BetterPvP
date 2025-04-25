package me.mykindos.betterpvp.champions.weapons.impl.runes.mitigation;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;

public class MitigationT1 extends MitigationBase {

    @Inject
    protected MitigationT1(Champions plugin) {
        super(plugin, "mitigation_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
