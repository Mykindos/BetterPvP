package me.mykindos.betterpvp.core.effects.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.LivingEntity;

@EqualsAndHashCode(callSuper = true)
@Data
public class EffectExpireEvent extends CustomEvent {

    private final LivingEntity target;
    private final Effect effect;
    private boolean notify;
    public EffectExpireEvent(LivingEntity target, Effect effect, boolean notify) {
        this.target = target;
        this.effect = effect;
        this.notify = notify;
    }
}
