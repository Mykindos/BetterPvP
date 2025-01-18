package me.mykindos.betterpvp.progression.profession.woodcutting.event;

import lombok.Getter;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

@Getter
public class PlayerStripLogEvent extends ProgressionWoodcuttingEvent {
    private final Block strippedLog;
    private final Result blockInteraction;
    private final Result itemInHandInteraction;


    public PlayerStripLogEvent(Player player, Block strippedLog, Result blockInteraction,
                               Result itemInHandInteraction) {
        super(player);
        this.strippedLog = strippedLog;
        this.blockInteraction = blockInteraction;
        this.itemInHandInteraction= itemInHandInteraction;
    }

    public boolean wasEventDeniedAndCancelled() {
        return isCancelled() && blockInteraction == Event.Result.DENY && itemInHandInteraction == Event.Result.DENY;
    }
}
