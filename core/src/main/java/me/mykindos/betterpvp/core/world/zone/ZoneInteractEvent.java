package me.mykindos.betterpvp.core.world.zone;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * The event-bus half of the zone rule system: fired by {@link ZoneManager#queryAccess} so any module can allow or deny
 * an action in a zone without being coupled to the zone's owner. This is the generalised replacement for clans'
 * former {@code TerritoryInteractEvent} — consumers query the owning clan in-place when they need it.
 * <p>
 * The carried {@link #result} starts as the verdict from the zone's attached {@link ZoneRule}s; handlers may override
 * it. {@link Event.Result#DEFAULT} means "no opinion — fall back to the caller's default behaviour".
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ZoneInteractEvent extends CustomEvent {

    private final Player player;
    private final Zone zone;
    private final @Nullable Block block;
    private final Location location;
    private final ZoneInteraction interaction;
    @Setter
    private Event.Result result;
    /** Whether the caller should inform the player about a denial (feedback message/sound). */
    @Setter
    private boolean inform = true;

    public ZoneInteractEvent(Player player, Zone zone, @Nullable Block block, Location location,
                             ZoneInteraction interaction, Event.Result result) {
        this.player = player;
        this.zone = zone;
        this.block = block;
        this.location = location;
        this.interaction = interaction;
        this.result = result;
    }
}
