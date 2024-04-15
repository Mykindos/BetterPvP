package me.mykindos.betterpvp.champions.weapons.impl.runes.resistance;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;

public class ResistanceT1 extends ResistanceBase{

    @Inject
    protected ResistanceT1(Champions plugin) {
        super(plugin, "resistance_rune_t1");
    }

    @Override
    public int getTier() {
        return 1;
    }
}
