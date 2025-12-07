package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player fuses a cannon {@link Cannon}.
 */
@Getter
public class CannonFuseEvent extends CustomCancellableEvent {

    private final @NotNull Cannon cannon;
    private final @NotNull Player player;

    public CannonFuseEvent(@NotNull Cannon cannon, @NotNull Player player) {
        this.cannon = cannon;
        this.player = player;
    }
}
