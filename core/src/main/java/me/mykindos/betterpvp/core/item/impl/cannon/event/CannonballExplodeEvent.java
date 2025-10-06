package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.Cannon;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a cannonball explodes.
 */
@Getter
@Setter
public class CannonballExplodeEvent extends CustomCancellableEvent {

    private final @NotNull Cannon cannon;
    private final @NotNull TNTPrimed cannonball;
    private final @NotNull Location location;
    private final @Nullable Player player;

    public CannonballExplodeEvent(@NotNull Cannon cannon, @NotNull TNTPrimed cannonball, @NotNull Location location, @Nullable Player player) {
        this.cannon = cannon;
        this.cannonball = cannonball;
        this.location = location;
        this.player = player;
    }
}
