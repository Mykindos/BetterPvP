package me.mykindos.betterpvp.clans.clans.events;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Called when a player interacts with a territory, such as breaking, placing or interacting with a block.
 */
@Getter
@Setter
public class TerritoryInteractEvent extends CustomEvent {

    private final Player player;
    private final Clan territoryOwner;
    private final Block block;
    private final InteractionType interactionType;
    private Result result;
    private boolean inform; // If the player should be informed of the result

    public TerritoryInteractEvent(Player player, Clan territoryOwner, Block block, Result result, InteractionType interactionType) {
        this.player = player;
        this.territoryOwner = territoryOwner;
        this.block = block;
        this.interactionType = interactionType;
        this.result = result;
        this.inform = true;
    }

    public enum InteractionType {
        BREAK,
        PLACE,
        INTERACT
    }

}
