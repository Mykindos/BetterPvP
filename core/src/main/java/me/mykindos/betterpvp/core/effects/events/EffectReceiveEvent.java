package me.mykindos.betterpvp.core.effects.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class EffectReceiveEvent extends CustomCancellableEvent {

    private final Player player;
    private final Effect effect;
}
