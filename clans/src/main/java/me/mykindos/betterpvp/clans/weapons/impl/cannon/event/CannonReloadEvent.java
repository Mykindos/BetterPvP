package me.mykindos.betterpvp.clans.weapons.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.CannonballWeapon;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player uses a {@link CannonballWeapon} to reload a {@link Cannon}.
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
