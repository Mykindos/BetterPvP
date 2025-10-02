package me.mykindos.betterpvp.core.loot.session;

import lombok.Value;
import me.mykindos.betterpvp.core.loot.LootProgress;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Represents a loot session.
 */
@Value
public class LootSession {

    @NotNull LootProgress progress;
    @NotNull Instant start = Instant.now();

}
