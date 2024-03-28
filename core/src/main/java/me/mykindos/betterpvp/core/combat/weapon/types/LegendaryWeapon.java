package me.mykindos.betterpvp.core.combat.weapon.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;

public interface LegendaryWeapon extends IWeapon {



    @Override
    default String getConfigName() {
        return "weapons/legendaries";
    }
}
