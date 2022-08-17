package me.mykindos.betterpvp.core.combat.death.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDeathEvent extends CustomCancellableEvent {

    // The person to receive the death message
    private final Player receiver;

    // The entity that was killed
    private final LivingEntity killed;
    private LivingEntity killer;
    private String reason;
    private String customDeathMessage;

}
