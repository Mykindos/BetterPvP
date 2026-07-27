package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a player aims a cannon {@link CannonProp}.
 */
@Getter
public class CannonAimEvent extends CustomCancellableEvent {

    private final @NotNull CannonProp cannon;
    private final @Nullable Player player;
    private final @NotNull Vector direction;

    public CannonAimEvent(@NotNull CannonProp cannon, @Nullable Player player, @NotNull Vector direction) {
        this.cannon = cannon;
        this.player = player;
        this.direction = direction;
    }
}
