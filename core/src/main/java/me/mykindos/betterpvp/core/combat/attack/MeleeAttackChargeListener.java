package me.mykindos.betterpvp.core.combat.attack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Stamps attack strength and critical hit state onto melee damage before any modifier reads it.
 */
@BPvPListener
@Singleton
public class MeleeAttackChargeListener implements Listener {

    private final AttackChargeService chargeService;
    private final CriticalHitService criticalHitService;

    @Inject
    private MeleeAttackChargeListener(AttackChargeService chargeService, CriticalHitService criticalHitService) {
        this.chargeService = chargeService;
        this.criticalHitService = criticalHitService;
    }

    // LOW so the weapon's damage stat (LOWEST) is already on the event when the critical hit is evaluated.
    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(DamageEvent event) {
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE) || event.isProjectile()) {
            return;
        }

        event.setAttackStrengthScale(chargeService.getCharge(event.getDamager()));
        event.setCritical(criticalHitService.isCritical(event));
    }
}
