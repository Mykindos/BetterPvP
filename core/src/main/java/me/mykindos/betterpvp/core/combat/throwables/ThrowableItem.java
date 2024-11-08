package me.mykindos.betterpvp.core.combat.throwables;

import lombok.Data;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

@Data
public class ThrowableItem {

    private final ThrowableListener listener;
    private final List<LivingEntity> immunes = new ArrayList<>();
    private final Item item;
    private final LivingEntity thrower;
    private final String name;
    private final long expireTime;
    private final long creationTime;

    private boolean collideGround;
    private boolean removeOnCollision;
    private boolean singleCollision;
    private double collisionRadius = 0.4;

    private Location lastLocation;

    @Setter
    private boolean canHitFriendlies;

    public ThrowableItem(ThrowableListener listener, Item item, LivingEntity thrower, String name, long expireTime) {
        this(listener, item, thrower, name, expireTime, false);
    }

    public ThrowableItem(ThrowableListener listener, Item item, LivingEntity thrower, String name, long expireTime, boolean removeOnCollision) {
        this.listener = listener;
        this.item = item;
        this.thrower = thrower;
        this.name = name;
        this.expireTime = System.currentTimeMillis() + expireTime;
        this.creationTime = System.currentTimeMillis();
        this.removeOnCollision = removeOnCollision;
        this.singleCollision = true;
        this.collideGround = false;
        this.canHitFriendlies = false;
        this.lastLocation = item.getLocation();
        item.setPickupDelay(Integer.MAX_VALUE);
    }

    public long getAge() {
        return System.currentTimeMillis() - this.creationTime;
    }
}