package me.mykindos.betterpvp.clans.weapons.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Called before a {@link Player} attempts to place a cannon at a {@link Location}.
 * Cancelling this event will prevent the player from placing the cannon.
 */
@Getter
public class PreCannonPlaceEvent extends CustomCancellableEvent {

    private final @NotNull Location cannonLocation;
    private final @NotNull Player player;

    public PreCannonPlaceEvent(@NotNull Location cannonLocation, @NotNull Player player) {
        this.cannonLocation = cannonLocation;
        this.player = player;
    }
}
