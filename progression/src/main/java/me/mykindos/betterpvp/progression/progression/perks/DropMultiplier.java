package me.mykindos.betterpvp.progression.progression.perks;

import me.mykindos.betterpvp.core.utilities.UtilMath;

public interface DropMultiplier {

    default int getExtraDrops(double chance) {
        int multiplier = (int) Math.floor(chance/100);
        double newChance = chance % 100;
        if (UtilMath.randomInt(0, 100) < newChance) {
            multiplier++;
        }
        return multiplier;
    }
}
