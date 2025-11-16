package me.mykindos.betterpvp.champions.champions.skills.data;

import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SkillWeapons {

    public static boolean isHolding(Player player, SkillType skillType) {
        final ItemStack item = player.getInventory().getItemInMainHand();
        return switch (skillType) {
            case SWORD -> UtilItem.isSword(item);
            case AXE -> UtilItem.isAxe(item);
            case BOW -> UtilItem.isRanged(item);
            default -> false; // Passives
        };
    }

    public static boolean hasBooster(Player player) {
        final ItemStack item = player.getInventory().getItemInMainHand();
        return isBooster(item.getType());
    }

    public static boolean isBooster(Material material) {
        return switch (material) {
            case GOLDEN_AXE, GOLDEN_SWORD, NETHERITE_SWORD, NETHERITE_AXE -> true;
            default -> false;
        };
    }

    public static SkillType getTypeFrom(ItemStack item) {
        if (UtilItem.isSword(item)) {
            return SkillType.SWORD;
        } else if (UtilItem.isAxe(item)) {
            return SkillType.AXE;
        } else if (UtilItem.isRanged(item)) {
            return SkillType.BOW;
        }

        return null;
    }

}
