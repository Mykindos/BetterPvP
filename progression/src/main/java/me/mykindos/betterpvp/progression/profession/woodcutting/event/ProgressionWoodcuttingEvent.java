package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@Getter
public abstract class ProgressionWoodcuttingEvent extends CustomEvent {
    private final Player player;

    public ProgressionWoodcuttingEvent(Player player) {
        this.player = player;
    }
}
