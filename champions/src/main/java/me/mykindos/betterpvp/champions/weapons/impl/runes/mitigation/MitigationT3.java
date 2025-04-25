package me.mykindos.betterpvp.champions.weapons.impl.runes.mitigation;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;

public class MitigationT3 extends MitigationBase {

    @Inject
    protected MitigationT3(Champions plugin) {
        super(plugin, "mitigation_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }
}
