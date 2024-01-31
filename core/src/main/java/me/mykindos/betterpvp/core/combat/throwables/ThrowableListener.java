package me.mykindos.betterpvp.core.combat.throwables;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public interface ThrowableListener {

    void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit);

    default void onThrowableHitGround(ThrowableItem throwableItem, LivingEntity thrower, Location location) {

    }
}
