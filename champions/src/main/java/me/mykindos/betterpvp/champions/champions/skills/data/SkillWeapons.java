package me.mykindos.betterpvp.champions.champions.skills.data;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

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
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        final ItemFactory itemFactory = plugin.getInjector().getInstance(ItemFactory.class);
        final Optional<ItemInstance> itemOpt = itemFactory.fromItemStack(item);
        if (itemOpt.isEmpty()) {
            return null;
        }

        final BaseItem baseItem = itemOpt.get().getBaseItem();
        final String namespacedKey = Objects.requireNonNull(itemFactory.getItemRegistry().getKey(baseItem)).toString();
        final String key = namespacedKey.toLowerCase().split(":")[1];
        return switch (key) {
            case "ancient_sword", "power_sword", "booster_sword", "standard_sword", "crude_sword", "rustic_sword" -> SkillType.SWORD;
            case "ancient_axe", "power_axe", "booster_axe", "standard_axe", "crude_axe", "rustic_axe" -> SkillType.AXE;
            case "bow", "crossbow" -> SkillType.BOW;
            default -> null;
        };
    }

}
