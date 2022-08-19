package me.mykindos.betterpvp.core.cooldowns.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
@Setter
public class CooldownDisplayEvent extends CustomCancellableEvent {

    private final Player player;
    private String cooldownName;

    public CooldownDisplayEvent(Player player) {
        this.player = player;
        this.cooldownName = "";
    }
}
