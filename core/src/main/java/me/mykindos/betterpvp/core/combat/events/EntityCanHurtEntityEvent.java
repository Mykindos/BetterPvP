package me.mykindos.betterpvp.core.combat.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class EntityCanHurtEntityEvent extends CustomEvent {

    private final LivingEntity damager;
    private final LivingEntity damagee;
    private Result result = Result.ALLOW;

    public boolean isAllowed() {
        return result == Result.ALLOW;
    }
}
