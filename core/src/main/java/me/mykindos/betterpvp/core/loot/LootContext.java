package me.mykindos.betterpvp.core.loot;

import lombok.Value;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * Defines the context for which a loot table is being evaluated.
 */
@Value
public class LootContext {

    /**
     * The player for whom the loot is being awarded.
     */
    @Nullable Player player;

    /**
     * The player's inventory, for convenience.
     */
    @Nullable PlayerInventory playerInventory;

    /**
     * The location at which the loot is being awarded.
     */
    @NotNull Location location;

    /**
     * The time at which the loot is being awarded.
     */
    Instant time;

    /**
     * The loot session for the player.
     */
    LootSession session;

    @NotNull String source;

    public LootContext(@Nullable Player player, @NotNull Location location, LootSession session, @NotNull String source) {
        this.player = player;
        this.playerInventory = player == null ? null : player.getInventory();
        this.location = location;
        this.session = session;
        this.source = source;
        this.time = Instant.now();
    }

}
