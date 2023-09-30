package me.mykindos.betterpvp.core.world.events;

import me.mykindos.betterpvp.core.framework.delayedactions.events.PlayerDelayedActionEvent;
import org.bukkit.entity.Player;

public class SpawnTeleportEvent extends PlayerDelayedActionEvent {

    public SpawnTeleportEvent(Player player, Runnable runnable) {
        super(player, runnable);
        this.titleText = "Teleport";
        this.subtitleText = "teleporting";
        this.countdown = true;
        this.countdownText = "Teleporting in";
    }
}
