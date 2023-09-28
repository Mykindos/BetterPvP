package me.mykindos.betterpvp.champions.weapons.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;

public interface CooldownWeapon extends IWeapon {

    double getCooldown();

    default boolean showCooldownFinished() {
        return true;
    }

}
