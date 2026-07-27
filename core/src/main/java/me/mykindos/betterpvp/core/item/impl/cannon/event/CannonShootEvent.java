package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonProjectile;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a cannon fires a shot. Carries the {@link CannonProjectile} rather than a raw {@code TNTPrimed} so
 * listeners work for every kind of ammunition, not only the TNT-backed shells.
 */
@Getter
public class CannonShootEvent extends CustomEvent {

    private final @NotNull CannonProp cannon;
    private final @NotNull CannonProjectile projectile;
    private final @Nullable Player player;

    public CannonShootEvent(@NotNull CannonProp cannon, @NotNull CannonProjectile projectile, @Nullable Player player) {
        this.cannon = cannon;
        this.projectile = projectile;
        this.player = player;
    }
}
