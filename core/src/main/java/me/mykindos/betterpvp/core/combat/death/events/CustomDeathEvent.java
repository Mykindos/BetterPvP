package me.mykindos.betterpvp.core.combat.death.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDeathEvent extends CustomEvent {

    private final LivingEntity killed;
    private LivingEntity killer;
    private String[] reason;

}
