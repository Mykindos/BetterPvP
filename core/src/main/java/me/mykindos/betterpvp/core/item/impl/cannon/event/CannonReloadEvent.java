package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.ammo.CannonAmmo;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player chambers a round in a {@link CannonProp}. Carries the {@link CannonAmmo} being loaded so
 * listeners can allow or refuse particular kinds of ammunition.
 */
@Getter
public class CannonReloadEvent extends CustomCancellableEvent {

    private final @NotNull CannonProp cannon;
    private final @NotNull Player player;
    private final @NotNull CannonAmmo ammo;

    public CannonReloadEvent(@NotNull CannonProp cannon, @NotNull Player player, @NotNull CannonAmmo ammo) {
        this.cannon = cannon;
        this.player = player;
        this.ammo = ammo;
    }
}
