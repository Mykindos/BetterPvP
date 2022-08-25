package me.mykindos.betterpvp.core.framework.delayedactions.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerDelayedActionEvent extends CustomCancellableEvent {

    private final Player player;
    private final Runnable runnable;
    private double delayInSeconds;

    protected String titleText;
    protected String subtitleText;
    protected boolean countdown;
    protected String countdownText;

}
