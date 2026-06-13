package me.mykindos.betterpvp.core.quest.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

/**
 * Fired by the {@code action.fire_event} quest primitive. Lets other systems
 * react to a quest beat (open a gate, spawn something) without the quest engine
 * knowing about them — the event bus is the decoupling seam.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class QuestMilestoneEvent extends CustomEvent {

    private final Player player;
    private final String key;

    public QuestMilestoneEvent(Player player, String key) {
        this.player = player;
        this.key = key;
    }
}
