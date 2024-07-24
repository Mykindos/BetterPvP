package me.mykindos.betterpvp.core.framework.events.kill;

import me.mykindos.betterpvp.core.framework.delayedactions.events.PlayerDelayedActionEvent;
import org.bukkit.entity.Player;

public class PlayerSuicideEvent extends PlayerDelayedActionEvent {

    public PlayerSuicideEvent(Player player, Runnable runnable) {
        super(player, runnable);
        this.titleText = "Suicide";
        this.subtitleText = "suiciding";
        this.countdown = true;
        this.countdownText = "";
    }
}
