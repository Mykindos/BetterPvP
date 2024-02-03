package me.mykindos.betterpvp.core.combat.throwables.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitGroundEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.RayTraceResult;

import java.util.List;

@BPvPListener
public class ThrowableItemListener implements Listener {

    @Inject
    private ThrowableHandler throwableHandler;

    @UpdateEvent(delay = 125)
    public void removeThrowables() {
        throwableHandler.getThrowables().removeIf(throwable -> {
            if (throwable.getExpireTime() - System.currentTimeMillis() <= 0 || !throwable.getItem().isValid()) {
                throwable.getItem().remove();
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onCollision(ThrowableHitEvent event) {
        if(event.isCancelled()) return;
        if (event.getThrowable().isRemoveOnCollision()) {
            event.getThrowable().getItem().remove();
        }
    }

    @UpdateEvent
    public void collisionCheck() {
        throwableHandler.getThrowables().forEach(throwable -> {
            checkGroundCollision(throwable);
            checkEntityCollision(throwable);
            throwable.setLastLocation(throwable.getItem().getLocation());
        });
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        throwableHandler.getThrowable(event.getItem()).ifPresent(throwable -> event.setCancelled(true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent event) {
        throwableHandler.getThrowable(event.getItem()).ifPresent(throwable -> event.setCancelled(true));
    }

    @EventHandler
    public void onMerge(ItemMergeEvent event) {
        throwableHandler.getThrowable(event.getEntity()).ifPresent(throwable -> event.setCancelled(true));
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        throwableHandler.getThrowables().removeIf(throwableItem -> throwableItem.getThrower().equals(event.getPlayer()));
    }

    private void checkGroundCollision(ThrowableItem throwable) {
        if (!throwable.isCollideGround() || throwable.getItem() == null || !throwable.getItem().isValid()
                || !UtilBlock.isGrounded(throwable.getItem())) {
            return;
        }

        throwableHandler.processThrowableHitGround(throwable, new ThrowableHitGroundEvent(throwable, throwable.getItem().getLocation()));
    }

    private void checkEntityCollision(ThrowableItem throwable) {
        if (throwable.getItem() == null || !throwable.getItem().isValid()) return;
        final Location location = throwable.getItem().getLocation().clone();
        final Location lastLocation = throwable.getLastLocation().clone();
        final double size = throwable.getCollisionRadius();

        // Attempt collision by nearby entities
        final List<LivingEntity> targets = UtilEntity.getNearbyEnemies(throwable.getThrower(), location, size);
        for (LivingEntity entity : targets) {
            if (throwable.getImmunes().contains(entity)) continue;
            throwableHandler.processThrowableHitEntity(throwable, new ThrowableHitEntityEvent(throwable, entity));
            if (throwable.isSingleCollision()) {
                break;
            }
        }

        if (!targets.isEmpty()) {
            return; // We hit an entity, so we don't need to fall back to ray trace
        }

        // Ray trace if all else fails
        UtilEntity.interpolateCollision(lastLocation, location, (float) size, entity -> {
            if (!(entity instanceof LivingEntity living) || living.equals(throwable.getThrower())) {
                return false;
            }

            return !throwable.getImmunes().contains(living);
        }).map(RayTraceResult::getHitEntity).map(LivingEntity.class::cast).ifPresent(entity -> {
            throwableHandler.processThrowableHitEntity(throwable, new ThrowableHitEntityEvent(throwable, entity));
        });
    }

}
