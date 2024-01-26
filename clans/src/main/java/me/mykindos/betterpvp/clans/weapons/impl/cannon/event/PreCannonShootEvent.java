package me.mykindos.betterpvp.clans.weapons.impl.cannon.event;

import lombok.Getter;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Called before a player fires a cannon {@link Cannon}.
 */
@Getter
public class PreCannonShootEvent extends CustomCancellableEvent {

    private final @NotNull Cannon cannon;
    private final @Nullable Player player; // can be null if player logs off while fusing
    private final @NotNull UUID playerId;

    public PreCannonShootEvent(@NotNull Cannon cannon, @Nullable Player player, @NotNull UUID playerId) {
        this.cannon = cannon;
        this.player = player;
        this.playerId = playerId;
    }
}
