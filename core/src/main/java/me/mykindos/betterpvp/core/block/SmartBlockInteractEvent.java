package me.mykindos.betterpvp.core.block;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

/**
 * Called before a player's click reaches a smart block. Cancelling it stops the click.
 */
@Getter
public class SmartBlockInteractEvent extends CustomCancellableEvent {

    private final Player player;
    private final SmartBlockInstance instance;
    private final Action action;

    public SmartBlockInteractEvent(Player player, SmartBlockInstance instance, Action action) {
        this.player = player;
        this.instance = instance;
        this.action = action;
    }

}
