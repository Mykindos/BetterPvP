package me.mykindos.betterpvp.core.framework.delayedactions.events;

import org.bukkit.entity.Player;

public class ClanHomeTeleportEvent extends PlayerDelayedActionEvent {

    public ClanHomeTeleportEvent(Player player, Runnable runnable) {
        super(player, runnable);
        this.titleText = "Teleport";
        this.subtitleText = "teleporting";
        this.countdown = true;
        this.countdownText = "Teleporting in";
    }
}
