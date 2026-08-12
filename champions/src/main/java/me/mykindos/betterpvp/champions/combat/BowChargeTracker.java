package me.mykindos.betterpvp.champions.combat;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Records how far the bow was drawn for every arrow it fires, so that damage handlers can scale an
 * arrow's damage by its draw strength at the moment it lands rather than at the moment it is loosed.
 */
@Singleton
@BPvPListener
public class BowChargeTracker implements Listener {

    private final Map<AbstractArrow, Float> charges = new WeakHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            charges.put(arrow, event.getForce());
        }
    }

    /**
     * The draw strength, 0 to 1, of the shot that fired this arrow. Anything that was not fired from a bow
     * - arrows a skill spawned directly, or an arrow whose shot was never seen - counts as fully drawn, so
     * charge scaling is a no-op for it.
     *
     * @param projectile the arrow to look up, may be null or a non-arrow projectile
     * @return the draw strength, or 1 if this projectile has no recorded shot
     */
    public float getCharge(@Nullable Entity projectile) {
        if (!(projectile instanceof AbstractArrow arrow)) {
            return 1.0f;
        }

        return charges.getOrDefault(arrow, 1.0f);
    }
}
