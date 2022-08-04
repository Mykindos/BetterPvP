package me.mykindos.betterpvp.core.combat.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class CustomDamageReductionEvent extends CustomEvent {

    private final CustomDamageEvent customDamageEvent;
    private double damage;
}
