package me.mykindos.betterpvp.clans.utilities;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class UtilClans {

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
