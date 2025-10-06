package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player fires a cannon {@link Cannon}.
 */
@Getter
public class CannonShootEvent extends CustomEvent {

    private final @NotNull Cannon cannon;
    private final @NotNull TNTPrimed cannonball;
    private final @Nullable Player player;

    public CannonShootEvent(@NotNull Cannon cannon, @NotNull TNTPrimed cannonball, @Nullable Player player) {
        this.cannon = cannon;
        this.cannonball = cannonball;
        this.player = player;
    }
}
