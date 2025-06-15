package me.mykindos.betterpvp.clans.item.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.clans.item.cannon.CannonballItem;
import me.mykindos.betterpvp.clans.item.cannon.model.Cannon;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
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
