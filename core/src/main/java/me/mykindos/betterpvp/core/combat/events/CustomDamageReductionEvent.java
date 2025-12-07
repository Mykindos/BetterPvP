package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDamageReductionEvent extends CustomEvent {

    private final DamageEvent DamageEvent;
    private final double initialDamage;
    private double damage;

    public CustomDamageReductionEvent(DamageEvent DamageEvent, double damage) {
        this.DamageEvent = DamageEvent;
        this.damage = damage;
        this.initialDamage = damage;
    }

    public enum Cause {
        ARMOR,
        POTION
    }
}
