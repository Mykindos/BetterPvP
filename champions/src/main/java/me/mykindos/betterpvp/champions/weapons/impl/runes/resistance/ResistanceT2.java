package me.mykindos.betterpvp.champions.weapons.impl.runes.resistance;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;

public class ResistanceT2 extends ResistanceBase{

    @Inject
    protected ResistanceT2(Champions plugin) {
        super(plugin, "resistance_rune_t2");
    }

    @Override
    public int getTier() {
        return 2;
    }
}
