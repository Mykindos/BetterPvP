package me.mykindos.betterpvp.champions.champions.skills.types;


import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface ToggleSkill extends IChampionsSkill {


    void toggle(Player player, int level);

    /** Whether this skill accepts the weapon being dropped. Defaults to the existing shared weapon rules. */
    default boolean canToggleWith(ItemStack item) {
        return true;
    }

}
