package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called the moment a rider leaves the barrel, once their landing point is locked in.
 */
@Getter
public class CannonLaunchPlayerEvent extends CustomEvent {

    /** The firing cannon, or {@code null} if it was destroyed between boarding and launch. */
    private final @Nullable CannonProp cannon;
    private final @NotNull Player player;
    private final @NotNull Location target;

    public CannonLaunchPlayerEvent(@Nullable CannonProp cannon, @NotNull Player player, @NotNull Location target) {
        this.cannon = cannon;
        this.player = player;
        this.target = target;
    }
}
