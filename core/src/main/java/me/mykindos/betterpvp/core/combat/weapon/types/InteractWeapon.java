package me.mykindos.betterpvp.core.combat.weapon.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import org.bukkit.entity.Player;

public interface InteractWeapon extends IWeapon {

    void activate(Player player);
    boolean canUse(Player player);

}
