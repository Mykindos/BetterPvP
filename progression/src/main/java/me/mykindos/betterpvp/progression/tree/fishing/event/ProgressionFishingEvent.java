package me.mykindos.betterpvp.progression.tree.fishing.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

public abstract class ProgressionFishingEvent extends CustomEvent {

    @Getter
    private final Player player;

    public ProgressionFishingEvent(Player player) {
        this.player = player;
    }
}
