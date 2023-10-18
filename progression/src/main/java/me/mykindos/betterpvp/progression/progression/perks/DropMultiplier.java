package me.mykindos.betterpvp.progression.progression.perks;

import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.Random;

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
