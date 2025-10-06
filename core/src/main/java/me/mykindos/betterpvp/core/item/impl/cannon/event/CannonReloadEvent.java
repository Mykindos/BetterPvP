package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.CannonballItem;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player uses a {@link CannonballItem} to reload a {@link Cannon}.
 */
@Getter
public class CannonReloadEvent extends CustomCancellableEvent {

    private final @NotNull Cannon cannon;
    private final @NotNull Player player;

    public CannonReloadEvent(@NotNull Cannon cannon, @NotNull Player player) {
        this.cannon = cannon;
        this.player = player;
    }
}
