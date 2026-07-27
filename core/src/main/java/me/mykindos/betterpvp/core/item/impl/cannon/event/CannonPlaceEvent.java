package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a {@link Player} places a cannon at a {@link Location}.
 */
@Getter
public class CannonPlaceEvent extends CustomEvent {

    private final @NotNull CannonProp cannon;
    private final @NotNull Location cannonLocation;
    private final @Nullable Player player;

    public CannonPlaceEvent(@NotNull CannonProp cannon, @NotNull Location cannonLocation, @Nullable Player player) {
        this.cannon = cannon;
        this.cannonLocation = cannonLocation;
        this.player = player;
    }
}
