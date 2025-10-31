package me.mykindos.betterpvp.game.impl.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player contributes points to their team (e.g., by capturing a point, grabbing a gem, etc.).
 */
@EqualsAndHashCode (callSuper = true)
@AllArgsConstructor
@Data
public class PlayerContributePointsEvent extends CustomEvent {

    /**
     * The player who contributed points.
     */
    final @NotNull Player player;

    /**
     * The number of points contributed by the player (in the case of a flag capture, it's only 1 point).
     */
    final int pointsContributed;
}
