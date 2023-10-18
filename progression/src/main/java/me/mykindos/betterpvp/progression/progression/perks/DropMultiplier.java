package me.mykindos.betterpvp.progression.progression.perks;

import me.mykindos.betterpvp.core.config.Config;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.Random;

public interface DropMultiplier {


    int minLevel = 0;

    int maxLevel = 1000;

    double increasePerLevel = 0.25;

    Random RANDOM = new Random();

    default int getMultiplier(double chance) {
        int multiplier = (int) Math.floor(chance/100);
        double newChance = chance % 100;
        if (RANDOM.nextInt(100) < newChance) {
            multiplier++;
        }
        return multiplier;
    }

    default int getMultiplier(int level) {
        return getMultiplier(level * increasePerLevel);
    }
}
