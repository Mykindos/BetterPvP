package me.mykindos.betterpvp.core.loot;

import com.google.common.base.Preconditions;
import lombok.Value;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Defines the context for which a loot table is being evaluated.
 */
@Value
public class LootContext {

    /**
     * The location at which the loot is being awarded.
     */
    @NotNull Location location;

    /**
     * The time at which the loot is being awarded.
     */
    @NotNull Instant time;

    /**
     * The loot session for the player.
     */
    @NotNull LootSession session;

    @NotNull String source;

    public LootContext(@NotNull LootSession session, @NotNull Location location, @NotNull String source) {
        Preconditions.checkArgument(!source.isEmpty(), "Source cannot be empty");
        this.location = location;
        this.session = session;
        this.source = source;
        this.time = Instant.now();
    }

}
