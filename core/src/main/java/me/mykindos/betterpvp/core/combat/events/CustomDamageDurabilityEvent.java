package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDamageDurabilityEvent extends CustomEvent {

    private final CustomDamageEvent customDamageEvent;
    private boolean damageeTakeDurability = true;
    private boolean damagerTakeDurability = true;

}
