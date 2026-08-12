package me.mykindos.betterpvp.champions.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.combat.BowChargeTracker;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Baseline arrow behaviour: a flat configured damage scaled by draw strength, no damage delay, and
 * arrows that never linger long enough to hit twice. Roles and skills layer on top of this.
 */
@Singleton
@BPvPListener
public class ArrowListener implements Listener {

    @Inject
    @Config(path = "combat.arrow-base-damage", defaultValue = "6.0")
    private double baseArrowDamage;

    @Inject
    @Config(path = "combat.crit-arrows", defaultValue = "false")
    private boolean critArrowsEnabled;

    private final Champions champions;
    private final BowChargeTracker bowChargeTracker;

    @Inject
    public ArrowListener(Champions champions, BowChargeTracker bowChargeTracker) {
        this.champions = champions;
        this.bowChargeTracker = bowChargeTracker;
    }

    /**
     * Strips the critical marker - and with it the crit particle trail - from fully drawn arrows when
     * crit arrows are turned off, so the visual never promises damage that will not land.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (critArrowsEnabled) return;
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            arrow.setCritical(false);
        }
    }

    /**
     * Replaces vanilla's velocity-derived arrow damage with our flat base damage, scaled by how far the
     * bow was drawn. The flat damage and the charge scaling must be applied together in one handler -
     * handlers at the same priority run in an order the JVM does not define, so splitting them lets the
     * flat damage land after the scaling and wipe it.
     * <p>
     * A fully drawn arrow is flagged critical by vanilla, which the damage pipeline turns into the usual
     * critical multiplier on base damage.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onArrowDamage(DamageEvent event) {
        AbstractArrow arrow = asArrow(event.getProjectile());
        if (arrow == null) return;

        event.setDamage(baseArrowDamage * bowChargeTracker.getCharge(arrow));
        Bukkit.broadcastMessage("damage: " + event.getDamage() + " crit: " + arrow.isCritical() + " charge: " + bowChargeTracker.getCharge(arrow) + "crit arrows: " + critArrowsEnabled);
        event.setCritical(critArrowsEnabled && arrow.isCritical());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileDelay(DamageEvent event) {
        if (event.getProjectile() == null) return;

        event.setDamageDelay(0);
    }

    /**
     * An arrow is spent once it lands a hit. Removing it at {@link EventPriority#MONITOR} leaves it intact
     * for every skill that reads the projectile off the damage event.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowHitEntity(DamageEvent event) {
        AbstractArrow arrow = asArrow(event.getProjectile());
        if (arrow == null || !arrow.isValid()) return;

        arrow.remove();
    }

    /**
     * Removes arrows that stuck in the ground or in a player.
     */
    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        AbstractArrow arrow = asArrow(event.getEntity());
        if (arrow == null) return;

        UtilServer.runTaskLater(champions, arrow::remove, 40L);
    }

    /**
     * The arrow this entity is, or null if it is not one. Tridents are arrows as far as Bukkit's hierarchy
     * is concerned, but they are a thrown weapon with their own damage and lifetime, so they never match.
     */
    @Nullable
    private AbstractArrow asArrow(@Nullable Entity entity) {
        return entity instanceof AbstractArrow arrow && !(arrow instanceof Trident) ? arrow : null;
    }
}
