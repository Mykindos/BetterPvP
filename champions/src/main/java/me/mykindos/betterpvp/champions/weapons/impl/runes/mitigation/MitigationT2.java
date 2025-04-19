package me.mykindos.betterpvp.champions.weapons.impl.runes.mitigation;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;

public class MitigationT2 extends MitigationBase {

    @Inject
    protected MitigationT2(Champions plugin) {
        super(plugin, "mitigation_rune_t2");
    }

    @Override
    public int getTier() {
        return 2;
    }
}
