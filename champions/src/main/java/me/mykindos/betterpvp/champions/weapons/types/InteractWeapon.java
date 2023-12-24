package me.mykindos.betterpvp.champions.weapons.types;

import me.mykindos.betterpvp.core.components.champions.weapons.IWeapon;
import org.bukkit.entity.Player;

public interface InteractWeapon extends IWeapon {

    void activate(Player player);
    boolean canUse(Player player);

    default boolean useShield(Player player) {
        return false;
    }

}
