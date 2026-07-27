package me.mykindos.betterpvp.core.item.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called before a player fires a cannon {@link CannonProp}.
 */
@Getter
public class PreCannonShootEvent extends CustomCancellableEvent {

    private final @NotNull CannonProp cannon;
    private final @Nullable Player player; // can be null if player logs off while fusing
    private final @NotNull UUID playerId;

    public PreCannonShootEvent(@NotNull CannonProp cannon, @Nullable Player player, @NotNull UUID playerId) {
        this.cannon = cannon;
        this.player = player;
        this.playerId = playerId;
    }
}
