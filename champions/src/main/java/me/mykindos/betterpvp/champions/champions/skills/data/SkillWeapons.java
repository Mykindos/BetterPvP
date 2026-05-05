package me.mykindos.betterpvp.champions.champions.skills.data;

import io.papermc.paper.persistence.PersistentDataContainerView;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class SkillWeapons {

    public static boolean isHolding(Player player, SkillType skillType) {
        final ItemStack item = player.getInventory().getItemInMainHand();
        final SkillType typeFromItem = getTypeFrom(item);
        return typeFromItem == skillType;
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
        final PersistentDataContainerView pdc = item.getPersistentDataContainer();
        if (!pdc.has(CoreNamespaceKeys.CUSTOM_ITEM_KEY)) {
            return null; // Not a custom item
        }

        final String namespacedKey = Objects.requireNonNull(pdc.get(CoreNamespaceKeys.CUSTOM_ITEM_KEY, PersistentDataType.STRING));
        final String key = namespacedKey.toLowerCase().split(":")[1];
        return switch (key) {
            case "ancient_sword", "power_sword", "booster_sword", "standard_sword", "crude_sword", "rustic_sword" -> SkillType.SWORD;
            case "ancient_axe", "power_axe", "booster_axe", "standard_axe", "crude_axe", "rustic_axe" -> SkillType.AXE;
            case "bow", "crossbow" -> SkillType.BOW;
            default -> null;
        };
    }

}
