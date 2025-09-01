package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomKnockbackEvent extends CustomCancellableEvent {

    private LivingEntity damagee, damager;
    private double damage;
    private final DamageEvent DamageEvent;
    private double multiplier = 1;

    private boolean canBypassMinimum = false;

    public CustomKnockbackEvent(LivingEntity damagee, LivingEntity damager, double damage, DamageEvent DamageEvent) {
        this.damagee = damagee;
        this.damager = damager;
        this.damage = damage;
        this.DamageEvent = DamageEvent;
    }

}
