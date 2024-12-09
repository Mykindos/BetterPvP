package me.mykindos.betterpvp.progression.profession.mining.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
public abstract class ProgressionMiningEvent extends CustomCancellableEvent {
    private final Player player;

    public ProgressionMiningEvent(Player player) {
        this.player = player;
    }
}
