package me.mykindos.betterpvp.core.item.component.impl.socketables;

import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import org.bukkit.Material;

import static me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup.getItemStack;

@SuppressWarnings("ALL")
public final class SocketableGroups {

    /**
     * Group representing all item types.
     */
    public static final SocketableGroup ALL = new SocketableGroup("Everything", item -> true);

    // <editor-fold desc="Equipment Types">

    /**
     * Group representing all armor pieces.
     */
    public static final SocketableGroup ARMOR = new SocketableGroup("Armor", item -> item.getItemGroup() == ItemGroup.ARMOR);

    /**
     * Group for helmets.
     */
    public static final SocketableGroup HELMET = new SocketableGroup("Helmets", item -> getItemStack(item).getType().name().contains("HELMET"));

    /**
     * Group for chestplates.
     */
    public static final SocketableGroup CHESTPLATE = new SocketableGroup("Chestplates", item -> getItemStack(item).getType().name().contains("CHESTPLATE"));

    /**
     * Group for leggings.
     */
    public static final SocketableGroup LEGGINGS = new SocketableGroup("Leggings", item -> getItemStack(item).getType().name().contains("LEGGINGS"));

    /**
     * Group for boots.
     */
    public static final SocketableGroup BOOTS = new SocketableGroup("Boots", item -> getItemStack(item).getType().name().contains("BOOTS"));

    // </editor-fold>

    // <editor-fold desc="Weapon Types">

    /**
     * Group representing all melee weapons.
     */
    public static final SocketableGroup MELEE_WEAPON = new SocketableGroup("Melee Weapons", item -> {
        if (item.getItemGroup() != ItemGroup.WEAPON) {
            return false;
        }

        final BaseItem baseItem = item instanceof ItemInstance itemInstance ? itemInstance.getBaseItem() : (BaseItem) item;
        return baseItem instanceof WeaponItem weaponItem && weaponItem.getGroups().contains(WeaponItem.Group.MELEE);
    });

    /**
     * Group representing all ranged weapons.
     */
    public static final SocketableGroup RANGED_WEAPON = new SocketableGroup("Ranged Weapons", item -> {
        if (item.getItemGroup() != ItemGroup.WEAPON) {
            return false;
        }

        final BaseItem baseItem = item instanceof ItemInstance itemInstance ? itemInstance.getBaseItem() : (BaseItem) item;
        return baseItem instanceof WeaponItem weaponItem && weaponItem.getGroups().contains(WeaponItem.Group.RANGED);
    });

    public static final SocketableGroup BOW = new SocketableGroup("Bows", item -> getItemStack(item).getType().name().contains("BOW"));

    // </editor-fold>

    // <editor-fold desc="Tool Types">

    /**
     * Group representing any tool.
     */
    public static final SocketableGroup TOOL = new SocketableGroup("Tools", item -> item.getItemGroup() == ItemGroup.TOOL || getItemStack(item).hasData(DataComponentTypes.TOOL));

    /**
     * Group for pickaxes.
     */
    public static final SocketableGroup PICKAXE = new SocketableGroup("Pickaxes", item -> getItemStack(item).getType().name().contains("_PICKAXE"));

    /**
     * Group for axes.
     */
    public static final SocketableGroup AXE = new SocketableGroup("Axes", item -> getItemStack(item).getType().name().contains("_AXE"));

    /**
     * Group for hoes.
     */
    public static final SocketableGroup HOE = new SocketableGroup("Hoes", item -> getItemStack(item).getType().name().contains("HOE"));

    /**
     * Group for shovels.
     */
    public static final SocketableGroup SHOVEL = new SocketableGroup("Shovels", item -> getItemStack(item).getType().name().contains("SHOVEL"));

    /**
     * Group for fishing rods.
     */
    public static final SocketableGroup FISHING_ROD = new SocketableGroup("Fishing Rods", item -> getItemStack(item).getType().equals(Material.FISHING_ROD));

    // </editor-fold>
    
}
