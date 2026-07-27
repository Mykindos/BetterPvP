package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonProjectile;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a cannon shot detonates.
 */
@Getter
@Setter
public class CannonballExplodeEvent extends CustomCancellableEvent {

    /** The firing cannon, or {@code null} if it was destroyed or unloaded while the shot was in the air. */
    private final @Nullable CannonProp cannon;
    private final @NotNull CannonProjectile projectile;
    private final @NotNull Location location;
    private final @Nullable Player player;

    public CannonballExplodeEvent(@Nullable CannonProp cannon, @NotNull CannonProjectile projectile,
                                  @NotNull Location location, @Nullable Player player) {
        this.cannon = cannon;
        this.projectile = projectile;
        this.location = location;
        this.player = player;
    }
}
