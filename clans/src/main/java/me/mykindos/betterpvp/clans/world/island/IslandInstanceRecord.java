package me.mykindos.betterpvp.clans.world.island;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A raw row loaded from the {@code island_instances} table by {@link IslandInstanceRepository#loadAll()}, before it
 * is resolved against a live {@link IslandTemplate}.
 */
@Value
public class IslandInstanceRecord {

    @NotNull UUID id;
    @NotNull String templateKey;
    @NotNull String world;
    @NotNull String state;
    long createdAt;

}
