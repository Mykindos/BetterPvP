package me.mykindos.betterpvp.core.effects.events;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
public class EffectClearEvent extends CustomCancellableEvent {

    private final Player player;

    public EffectClearEvent(Player player) {
        this.player = player;
    }
}
