package me.mykindos.betterpvp.core.item.model;

import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.inventory.CreativeCategory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * Represents a {@link BaseItem} with a custom name and rarity.
 */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VanillaItem that)) return false;
        if (!super.equals(o)) return false;

        // If the items are custom models, compare their metadata
        if (that.getModel().hasItemMeta() || getModel().hasItemMeta()) {
            if (!that.getModel().isSimilar(getModel())) {
                return false;
            }
        }

        // Otherwise, compare the item types.
        else if (that.getModel().getType() != getModel().getType()) {
            return false;
        }

        return getItemGroup() == that.getItemGroup()
                && getSerializableComponents().equals(that.getSerializableComponents())
                && getComponents().equals(that.getComponents());
    }
}
