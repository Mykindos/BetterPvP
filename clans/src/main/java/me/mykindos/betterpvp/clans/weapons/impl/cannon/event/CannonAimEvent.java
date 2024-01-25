package me.mykindos.betterpvp.clans.weapons.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player aims a cannon {@link Cannon}.
 */
@Getter
public class CannonAimEvent extends CustomCancellableEvent {

    private final @NotNull Cannon cannon;
    private final @Nullable Player player;
    private final @NotNull Vector direction;

    public CannonAimEvent(@NotNull Cannon cannon, @Nullable Player player, @NotNull Vector direction) {
        this.cannon = cannon;
        this.player = player;
        this.direction = direction;
    }
}
