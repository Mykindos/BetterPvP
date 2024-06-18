package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@Getter
public abstract class ProgressionWoodcuttingEvent extends CustomCancellableEvent {
    private final Player player;

    public ProgressionWoodcuttingEvent(Player player) {
        this.player = player;
    }
}
