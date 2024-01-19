package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDamageReductionEvent extends CustomEvent {

    private final CustomDamageEvent customDamageEvent;
    private final double initialDamage;
    private double damage;

    public CustomDamageReductionEvent(CustomDamageEvent customDamageEvent, double damage) {
        this.customDamageEvent = customDamageEvent;
        this.damage = damage;
        this.initialDamage = damage;
    }
}
