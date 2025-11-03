package me.mykindos.betterpvp.core.item.component.impl.runes;

import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.AllArgsConstructor;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.item.model.WeaponItem.Group;
import org.bukkit.inventory.ItemStack;

import java.util.function.Predicate;

/**
 * Represents a group of runes that can be applied to a certain
 * equipment type.
 */
@SuppressWarnings("UnstableApiUsage")
@AllArgsConstructor
public enum RuneGroup {

    /**
     * Any item that has {@link ItemGroup#ARMOR}
     */
    ARMOR("Armor", item -> item.getItemGroup() == ItemGroup.ARMOR),

    /**
     * Any item that has {@link ItemGroup#WEAPON}, extends {@link WeaponItem} and has group {@link Group#MELEE}
     */
    MELEE_WEAPON("Melee Weapons", item -> {
        if (item.getItemGroup() != ItemGroup.WEAPON) {
            return false;
        }

        final BaseItem baseItem = item instanceof ItemInstance itemInstance ? itemInstance.getBaseItem() : (BaseItem) item;
        return baseItem instanceof WeaponItem weaponItem && weaponItem.getGroups().contains(Group.MELEE);
    }),

    /**
     * Any item that has {@link ItemGroup#WEAPON}, extends {@link WeaponItem} and has group {@link Group#RANGED}
     */
    RANGED_WEAPON("Ranged Weapons", item -> {
        if (item.getItemGroup() != ItemGroup.WEAPON) {
            return false;
        }

        final BaseItem baseItem = item instanceof ItemInstance itemInstance ? itemInstance.getBaseItem() : (BaseItem) item;
        return baseItem instanceof WeaponItem weaponItem && weaponItem.getGroups().contains(Group.RANGED);
    }),

    /**
     * Any item that has the DataComponentTypes.TOOL data key or has {@link ItemGroup#TOOL}
     */
    TOOL("Tools", item -> {
        final ItemStack itemStack;
        final boolean tool;
        if (item instanceof ItemInstance itemInstance) {
            itemStack = itemInstance.createItemStack();
            tool = itemInstance.getBaseItem().getItemGroup() == ItemGroup.TOOL;
        } else if (item instanceof BaseItem baseItem) {
            itemStack = baseItem.getModel();
            tool = baseItem.getItemGroup() == ItemGroup.TOOL;
        } else {
            throw new IllegalArgumentException("Item must be an ItemInstance or BaseItem");
        }

        return tool || itemStack.hasData(DataComponentTypes.TOOL);
    });

    private final String displayName;
    private final Predicate<Item> itemPredicate;

    public String getDisplayName() {
        return displayName;
    }

    public boolean canApply(Item item) {
        return itemPredicate.test(item);
    }

}
