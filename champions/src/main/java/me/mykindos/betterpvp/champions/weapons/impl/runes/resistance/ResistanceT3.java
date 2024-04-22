package me.mykindos.betterpvp.champions.weapons.impl.runes.resistance;

import com.google.inject.Inject;
import me.mykindos.betterpvp.champions.Champions;

public class ResistanceT3 extends ResistanceBase{

    @Inject
    protected ResistanceT3(Champions plugin) {
        super(plugin, "resistance_rune_t3");
    }

    @Override
    public int getTier() {
        return 3;
    }
}
