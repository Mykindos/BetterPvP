package me.mykindos.betterpvp.clans.world.island;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * The server-qualified result of an {@link IslandAllocator} allocation: enough to identify an
 * {@link IslandInstance} and find your way to it, regardless of which server actually holds it.
 */
@Value
public class IslandHandle {

    @NotNull UUID instanceId;
    @NotNull String templateKey;
    @NotNull String server;
    @NotNull String world;

}
