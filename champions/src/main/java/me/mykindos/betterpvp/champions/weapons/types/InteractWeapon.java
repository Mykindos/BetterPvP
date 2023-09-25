package me.mykindos.betterpvp.champions.weapons.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import org.bukkit.event.block.Action;

public interface InteractWeapon extends IWeapon {

    Action[] getActions();

}
