package me.mykindos.betterpvp.core.combat.weapon.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;

public interface CooldownWeapon extends IWeapon {

    double getCooldown();

    default boolean showCooldownFinished() {
        return true;
    }

    default boolean showCooldownOnItem() {
        return false;
    }

}
