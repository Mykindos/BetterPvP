package me.mykindos.betterpvp.core.framework.blockbreak.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player-driven script (item ability, profession passive, environmental event, etc.)
 * is about to place or replace a block <em>without</em> the player manually placing it. The classic
 * vanilla {@link org.bukkit.event.block.BlockPlaceEvent} only fires for direct hand-placement;
 * this event covers the "an ability of mine just spawned a block somewhere" case.
 *
 * <p>Listeners may cancel to veto the placement. The firing code is responsible for honoring the
 * cancellation and not mutating the world.
 *
 * <p>The {@link #getSource() source} is a free-form identifier the firing code supplies — by
 * convention {@code "<module>:<item-or-system>/<subsystem>"}, e.g.
 * {@code "progression:deep_resonator/vein_echo"}. Listeners that care about origin should match
 * on this string rather than introducing compile-time coupling to the firing module.
 *
 * <p>{@link #getPreviousData() previousData} captures the block's state at fire time (often
 * {@link org.bukkit.Material#AIR} when the script is reacting to a fresh break);
 * {@link #getReplacementData() replacementData} is the target state.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ScriptedBlockPlaceEvent extends CustomCancellableEvent {

    private final @NotNull Player player;
    private final @NotNull Block block;
    private final @NotNull BlockData previousData;
    private final @NotNull BlockData replacementData;
    private final @NotNull String source;

    /**
     * Standard Bukkit access-control tri-state. Listeners can flip to {@link Event.Result#ALLOW}
     * to claim authority over this placement (suppressing later access checks) or
     * {@link Event.Result#DENY} as a soft veto. The firing code only enforces
     * {@link #isCancelled()}, so a higher-priority listener should still call
     * {@link #setCancelled(boolean)} if it converts a DENY into a hard rejection.
     */
    @Setter private @NotNull Event.Result result = Event.Result.DEFAULT;

    public ScriptedBlockPlaceEvent(@NotNull Player player,
                                   @NotNull Block block,
                                   @NotNull BlockData previousData,
                                   @NotNull BlockData replacementData,
                                   @NotNull String source) {
        this.player = player;
        this.block = block;
        this.previousData = previousData;
        this.replacementData = replacementData;
        this.source = source;
    }

    @Override
    public void cancel(String reason) {
        this.setCancelReason(reason);
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.result = cancelled ? Event.Result.DENY : Event.Result.DEFAULT;
    }

    @Override
    public boolean isCancelled() {
        return this.result == Event.Result.DENY;
    }
}
