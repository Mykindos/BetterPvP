package me.mykindos.betterpvp.core.item.component.impl.runes;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;

import static me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroup.getItemStack;

@SuppressWarnings("ALL")
public final class RuneGroups {

    /**
     * Group representing all item types.
     */
    public static final RuneGroup ALL = new RuneGroup("Everything", item -> true);

    // <editor-fold desc="Equipment Types">

    /**
     * Group representing all armor pieces.
     */
    public static final RuneGroup ARMOR = new RuneGroup("Armor", item -> item.getItemGroup() == ItemGroup.ARMOR);

    /**
     * Group for helmets.
     */
    public static final RuneGroup HELMET = new RuneGroup("Helmets", item -> getItemStack(item).getType().name().contains("HELMET"));

    /**
     * Group for chestplates.
     */
    public static final RuneGroup CHESTPLATE = new RuneGroup("Chestplates", item -> getItemStack(item).getType().name().contains("CHESTPLATE"));

    /**
     * Group for leggings.
     */
    public static final RuneGroup LEGGINGS = new RuneGroup("Leggings", item -> getItemStack(item).getType().name().contains("LEGGINGS"));

    /**
     * Group for boots.
     */
    public static final RuneGroup BOOTS = new RuneGroup("Boots", item -> getItemStack(item).getType().name().contains("BOOTS"));

    // </editor-fold>

    // <editor-fold desc="Weapon Types">

    /**
     * Group representing all melee weapons.
     */
    public static final RuneGroup MELEE_WEAPON = new RuneGroup("Melee Weapons", item -> {
        if (item.getItemGroup() != ItemGroup.WEAPON) {
            return false;
        }

        final BaseItem baseItem = item instanceof ItemInstance itemInstance ? itemInstance.getBaseItem() : (BaseItem) item;
        return baseItem instanceof WeaponItem weaponItem && weaponItem.getGroups().contains(WeaponItem.Group.MELEE);
    });

    /**
     * Group representing all ranged weapons.
     */
    public static final RuneGroup RANGED_WEAPON = new RuneGroup("Ranged Weapons", item -> {
        if (item.getItemGroup() != ItemGroup.WEAPON) {
            return false;
        }

        final BaseItem baseItem = item instanceof ItemInstance itemInstance ? itemInstance.getBaseItem() : (BaseItem) item;
        return baseItem instanceof WeaponItem weaponItem && weaponItem.getGroups().contains(WeaponItem.Group.RANGED);
    });

    // </editor-fold>

    // <editor-fold desc="Tool Types">

    /**
     * Group representing any tool.
     */
    public static final RuneGroup TOOL = new RuneGroup("Tools", item -> item.getItemGroup() == ItemGroup.TOOL || getItemStack(item).hasData(DataComponentTypes.TOOL));

    /**
     * Group for pickaxes.
     */
    public static final RuneGroup PICKAXE = new RuneGroup("Pickaxes", item -> getItemStack(item).getType().name().contains("_PICKAXE"));

    /**
     * Group for axes.
     */
    public static final RuneGroup AXE = new RuneGroup("Axes", item -> getItemStack(item).getType().name().contains("_AXE"));

    /**
     * Group for hoes.
     */
    public static final RuneGroup HOE = new RuneGroup("Hoes", item -> getItemStack(item).getType().name().contains("HOE"));

    /**
     * Group for shovels.
     */
    public static final RuneGroup SHOVEL = new RuneGroup("Shovels", item -> getItemStack(item).getType().name().contains("SHOVEL"));

    /**
     * Group for fishing rods.
     */
    public static final RuneGroup FISHING_ROD = new RuneGroup("Fishing Rods", item -> getItemStack(item).getType().equals(Material.FISHING_ROD));

    // </editor-fold>
    
}
