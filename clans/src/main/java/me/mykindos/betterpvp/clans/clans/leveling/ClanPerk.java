package me.mykindos.betterpvp.clans.clans.leveling;

import me.mykindos.betterpvp.clans.clans.Clan;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

public interface ClanPerk {

    /**
     * Get the name of the perk
     * @return The name of the perk
     */
    String getName();

    /**
     * Get the minimum level required to unlock the perk
     * @return The minimum level required to unlock the perk
     */
    int getMinimumLevel();

    /**
     * Get the description of the perk
     * @return The description of the perk
     */
    Component[] getDescription();

    /**
     * Get the icon of the perk
     * @return The icon of the perk in menus
     */
    ItemStack getIcon();

    /**
     * Check if the clan has the perk
     * @param clan The clan to check
     * @return True if the clan has the perk
     */
    default boolean hasPerk(Clan clan) {
        return clan.getLevel() >= getMinimumLevel();
    }

}
