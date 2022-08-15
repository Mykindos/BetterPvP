package me.mykindos.betterpvp.champions.utilities;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class UtilChampions {

    public static boolean isUsableWithShield(ItemStack item) {

        if (item.getType().name().contains("_SWORD")) {
            return true;
        }

        // Legendaries
        if (item.getType() == Material.MUSIC_DISC_MELLOHI || item.getType() == Material.MUSIC_DISC_STRAD
                || item.getType() == Material.MUSIC_DISC_CAT) {
            return true;
        }

        return false;
    }

}
