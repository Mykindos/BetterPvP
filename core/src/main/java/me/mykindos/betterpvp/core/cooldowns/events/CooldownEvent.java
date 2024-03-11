package me.mykindos.betterpvp.core.cooldowns.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.cooldowns.Cooldown;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class CooldownEvent extends CustomCancellableEvent {

    private final Player player;
    private final Cooldown cooldown;

}
