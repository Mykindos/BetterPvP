package me.mykindos.betterpvp.core.item.impl.interaction.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Fired after {@link me.mykindos.betterpvp.core.item.impl.interaction.TreeFellerInteraction}
 * collects a tree and before any collected blocks are broken. Listeners in other modules
 * (e.g., progression's EnchantedLumberfall)
 * can react to this event without depending on a progression-specific event class.
 */
@Getter
public class TreeFellerEvent extends CustomEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    /**
     * The location of the first leaf block encountered during tree-felling.
     * Null when the tree had no leaves (or all leaves were player-placed).
     * Intended for drop-on-leaves perks like EnchantedLumberfall.
     */
    @Nullable
    private final Location leafActivationLocation;

    private final Location initialLogLocation;
    private final Material initialLogType;

    /**
     * The mutable list of blocks that will be broken after this event finishes.
     * Listeners may remove blocks from this list to prevent Tree Feller from breaking them.
     */
    private final List<Block> blocks;

    public TreeFellerEvent(@NotNull Player player,
                           @Nullable Location leafActivationLocation,
                           @NotNull Location initialLogLocation,
                           @NotNull Material initialLogType,
                           @NotNull List<Block> blocks) {
        this.player = player;
        this.leafActivationLocation = leafActivationLocation;
        this.initialLogLocation = initialLogLocation;
        this.initialLogType = initialLogType;
        this.blocks = blocks;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
