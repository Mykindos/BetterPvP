package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called once a rider has come down and been returned to their normal game mode.
 */
@Getter
public class CannonLandEvent extends CustomEvent {

    /** The cannon that launched them, or {@code null} if it did not survive the flight. */
    private final @Nullable CannonProp cannon;
    private final @NotNull Player player;
    private final @NotNull Location location;

    public CannonLandEvent(@Nullable CannonProp cannon, @NotNull Player player, @NotNull Location location) {
        this.cannon = cannon;
        this.player = player;
        this.location = location;
    }
}
