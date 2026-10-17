package me.mykindos.betterpvp.core.item.model;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Equippable;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Represents a {@link BaseItem} with a custom name and rarity.
 */
public class VanillaItem extends BaseItem {

    public VanillaItem(Material type, ItemRarity rarity) {
        this(Component.translatable(type.translationKey()), type, rarity);
    }

    public VanillaItem(String name, Material type, ItemRarity rarity) {
        this(Component.text(name), type, rarity);
    }

    public VanillaItem(Component component, Material type, ItemRarity rarity) {
        super(component, ItemStack.of(type), groupOf(ItemStack.of(type)), rarity);
    }

    public VanillaItem(String name, ItemStack proxy, ItemRarity rarity) {
        this(Component.text(name), proxy, rarity);
    }

    public VanillaItem(Component name, ItemStack proxy, ItemRarity rarity) {
        super(name, proxy, groupOf(proxy), rarity);
    }

    /** Weapons come before tools, since swords also carry the tool component. */
    private static ItemGroup groupOf(ItemStack item) {
        if (item.getType().isBlock()) {
            return ItemGroup.BLOCK;
        }
        if (UtilItem.isWeapon(item)) {
            return ItemGroup.WEAPON;
        }
        final Equippable equippable = item.getData(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && equippable.slot().isArmor()) {
            return ItemGroup.ARMOR;
        }
        if (item.hasData(DataComponentTypes.TOOL)) {
            return ItemGroup.TOOL;
        }
        if (item.hasData(DataComponentTypes.FOOD) || item.hasData(DataComponentTypes.CONSUMABLE)) {
            return ItemGroup.CONSUMABLE;
        }
        return ItemGroup.MATERIAL;
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
