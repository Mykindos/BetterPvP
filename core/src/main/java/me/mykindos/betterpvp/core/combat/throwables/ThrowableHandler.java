package me.mykindos.betterpvp.core.combat.throwables;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitGroundEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Singleton
public class ThrowableHandler {

    private final List<ThrowableItem> throwables = new ArrayList<>();

    public void addThrowable(ThrowableListener listener, Item item, LivingEntity entity, String name, long expire){
        addThrowable(listener, item, entity, name, expire, false);
    }

    public void addThrowable(ThrowableListener listener, Item item, LivingEntity entity, String name, long expire, boolean removeOnCollision){
        addThrowable(new ThrowableItem(listener, item, entity, name, expire, removeOnCollision));
    }

    public void addThrowable(ThrowableItem throwableItem){
        throwables.add(throwableItem);
    }

    public Optional<ThrowableItem> getThrowable(Item item) {
        return throwables.stream().filter(throwable -> throwable.getItem().equals(item)).findFirst();
    }

    public void processThrowableHitEntity(ThrowableItem throwableItem, ThrowableHitEntityEvent event) {
        ThrowableHitEntityEvent throwableHitEntityEvent = UtilServer.callEvent(event);
        if (!throwableHitEntityEvent.isCancelled()) {
            throwableItem.getListener().onThrowableHit(throwableItem, throwableItem.getThrower(), throwableHitEntityEvent.getCollision());
        }
    }

    public void processThrowableHitGround(ThrowableItem throwableItem, ThrowableHitGroundEvent event) {
        ThrowableHitGroundEvent throwableHitGroundEvent = UtilServer.callEvent(event);
        if (!throwableHitGroundEvent.isCancelled()) {
            throwableItem.getListener().onThrowableHitGround(throwableItem, throwableItem.getThrower(), throwableItem.getLastLocation());
        }
    }

}
