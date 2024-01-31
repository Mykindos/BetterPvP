package me.mykindos.betterpvp.core.combat.weapon.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import org.bukkit.inventory.meta.ItemMeta;

public interface LegendaryWeapon extends IWeapon {

    default void onInitialize(ItemMeta meta) {};

}
