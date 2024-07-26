package me.mykindos.betterpvp.clans.weapons.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a {@link Player} places a cannon at a {@link Location}.
 */
@Getter
public class CannonPlaceEvent extends CustomEvent {

    private final @NotNull Cannon cannon;
    private final @NotNull Location cannonLocation;
    private final @Nullable Player player;

    public CannonPlaceEvent(@NotNull Cannon cannon, @NotNull Location cannonLocation, @Nullable Player player) {
        this.cannon = cannon;
        this.cannonLocation = cannonLocation;
        this.player = player;
    }
}
