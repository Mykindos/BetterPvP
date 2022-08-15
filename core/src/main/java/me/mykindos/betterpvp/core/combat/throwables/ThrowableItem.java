package me.mykindos.betterpvp.core.combat.throwables;

import lombok.Data;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

@Data
public class ThrowableItem {

    private final Item item;
    private final LivingEntity thrower;
    private final String name;
    private final long expireTime;
    private final List<LivingEntity> immune;
    private final boolean checkHead;

    private boolean collideGround;
    private boolean removeOnCollision;
    private boolean singleCollision;

    public ThrowableItem(Item item, LivingEntity thrower, String name, long expireTime) {
        this(item, thrower, name, expireTime, false, false);
    }

    public ThrowableItem(Item item, LivingEntity thrower, String name, long expireTime, boolean checkHead){
        this(item, thrower, name, expireTime, checkHead, false);
    }

    public ThrowableItem(Item item, LivingEntity thrower, String name, long expireTime, boolean checkHead, boolean removeOnCollision){
        this.item = item;
        this.thrower = thrower;
        this.name = name;
        this.expireTime = System.currentTimeMillis() + expireTime;
        this.checkHead = checkHead;
        this.removeOnCollision = removeOnCollision;
        this.singleCollision = true;
        this.collideGround = false;
        item.setPickupDelay(Integer.MAX_VALUE);
        immune = new ArrayList<>();
    }

    public boolean isCheckingHead() {
        return checkHead;
    }

    public List<LivingEntity> getImmunes() {
        return immune;
    }

}
