package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;

@EqualsAndHashCode(callSuper = true)
@Data
public class PreDamageEvent extends CustomCancellableEvent {

    private final DamageEvent damageEvent;

}
