package me.mykindos.betterpvp.clans.combat.throwables.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.combat.throwables.ThrowableHandler;
import me.mykindos.betterpvp.clans.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.clans.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.clans.combat.throwables.events.ThrowableHitEvent;
import me.mykindos.betterpvp.clans.combat.throwables.events.ThrowableHitGroundEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

@BPvPListener
public class ThrowableListener implements Listener {

    private final ThrowableHandler throwableHandler;

    @Inject
    public ThrowableListener(ThrowableHandler throwableHandler) {
        this.throwableHandler = throwableHandler;
    }

    @UpdateEvent(delay = 125)
    public void removeThrowables() {
        throwableHandler.getThrowables().removeIf(throwable -> {
            if (throwable.getExpireTime() - System.currentTimeMillis() <= 0) {
                throwable.getItem().remove();
                return true;
            }
            return false;
        });

    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (throwableHandler.getThrowable(event.getItem()).isPresent()) {
            event.setCancelled(true);
        }

    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (throwableHandler.getThrowable(event.getItem()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMerge(ItemMergeEvent event) {
        if (throwableHandler.getThrowable(event.getEntity()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @UpdateEvent
    public void collisionCheck() {
        throwableHandler.getThrowables().forEach(throwable -> {
            checkGroundCollision(throwable);
            checkEntityCollision(throwable);
        });
    }

    private void checkGroundCollision(ThrowableItem throwable) {
        if (!throwable.isCollideGround()) return;
        if (throwable.getItem() == null || throwable.getItem().isDead()) return;
        if (UtilBlock.isGrounded(throwable.getItem())) {
            UtilServer.callEvent(new ThrowableHitGroundEvent(throwable));
        }
    }

    private void checkEntityCollision(ThrowableItem throwable) {
        if (throwable.getItem() == null || throwable.getItem().isDead()) return;
        Location location = throwable.getItem().getLocation().clone();
        if (!doCollision(throwable, location, 1.5)) {
            if (throwable.isCheckingHead()) {
                doCollision(throwable, location.add(0, 1, 0), 1);
            }
        }
    }

    private boolean doCollision(ThrowableItem throwable, Location location, double distance) {
        List<LivingEntity> targets = UtilEntity.getNearbyEntities(throwable.getThrower(), location, distance);
        for (LivingEntity entity : targets) {
            if (throwable.getImmune().contains(entity)) continue;
            UtilServer.callEvent(new ThrowableHitEntityEvent(throwable, entity));
            if (throwable.isSingleCollision()) {
                break;
            }
        }

        return !targets.isEmpty();
    }

    @EventHandler
    public void onCollision(ThrowableHitEvent event) {
        if (event.getThrowable().isRemoveOnCollision()) {
            event.getThrowable().getItem().remove();
        }
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        throwableHandler.getThrowables().removeIf(throwableItem -> throwableItem.getThrower().equals(event.getPlayer()));
    }
}
