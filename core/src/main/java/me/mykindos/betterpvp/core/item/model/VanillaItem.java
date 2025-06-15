package me.mykindos.betterpvp.core.item.model;

import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Represents a {@link BaseItem} with a custom name and rarity.
 */
@EqualsAndHashCode(callSuper = true)
public class VanillaItem extends BaseItem {

    public VanillaItem(String name, ItemStack proxy, ItemRarity rarity) {
        super(name, proxy, switch (Objects.requireNonNullElse(proxy.getType().getCreativeCategory(), CreativeCategory.MISC)) {
            case BUILDING_BLOCKS, DECORATIONS -> ItemGroup.BLOCK;
            case REDSTONE, TRANSPORTATION, MISC, BREWING -> ItemGroup.MATERIAL;
            case FOOD -> ItemGroup.CONSUMABLE;
            case TOOLS -> ItemGroup.TOOL;
            case COMBAT -> UtilItem.isWeapon(proxy) ? ItemGroup.WEAPON : ItemGroup.ARMOR;
        }, rarity);
    }
}
