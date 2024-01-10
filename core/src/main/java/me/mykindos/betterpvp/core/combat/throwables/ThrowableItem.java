package me.mykindos.betterpvp.core.combat.throwables;

import lombok.Data;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

@Data
public class ThrowableItem {

    private final List<LivingEntity> immunes = new ArrayList<>();
    private final Item item;
    private final LivingEntity thrower;
    private final String name;
    private final long expireTime;

    private boolean collideGround;
    private boolean removeOnCollision;
    private boolean singleCollision;
    private double collisionRadius = 0.4;

    private Location lastLocation;

    public ThrowableItem(Item item, LivingEntity thrower, String name, long expireTime) {
        this(item, thrower, name, expireTime, false);
    }

    public ThrowableItem(Item item, LivingEntity thrower, String name, long expireTime, boolean removeOnCollision) {
        this.item = item;
        this.thrower = thrower;
        this.name = name;
        this.expireTime = System.currentTimeMillis() + expireTime;
        this.removeOnCollision = removeOnCollision;
        this.singleCollision = true;
        this.collideGround = false;
        this.lastLocation = item.getLocation();
        item.setPickupDelay(Integer.MAX_VALUE);
    }

}
