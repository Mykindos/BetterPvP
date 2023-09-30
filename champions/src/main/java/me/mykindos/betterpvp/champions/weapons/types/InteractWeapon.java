package me.mykindos.betterpvp.champions.weapons.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public interface InteractWeapon extends IWeapon {

    Action[] getActions();

    void activate(Player player);
    boolean canUse(Player player);

}
