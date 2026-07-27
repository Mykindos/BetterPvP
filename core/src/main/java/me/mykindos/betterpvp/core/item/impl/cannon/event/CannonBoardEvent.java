package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a player climbs into a passenger cannon. Cancelling keeps them out - this is the hook other modules
 * use to apply ownership or region rules to a launch, the same way {@link CannonFuseEvent} gates a shot.
 */
@Getter
public class CannonBoardEvent extends CustomCancellableEvent {

    private final @NotNull CannonProp cannon;
    private final @NotNull Player player;

    public CannonBoardEvent(@NotNull CannonProp cannon, @NotNull Player player) {
        this.cannon = cannon;
        this.player = player;
    }
}
