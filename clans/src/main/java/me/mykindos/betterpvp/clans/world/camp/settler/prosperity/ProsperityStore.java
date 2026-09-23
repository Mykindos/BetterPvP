package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/** Where each camp's Prosperity is kept, so it can be ranked with the camp not loaded. */
public interface ProsperityStore {

    /** Records {@code clanId}'s Prosperity, replacing what was there. */
    @NotNull CompletableFuture<Void> save(long clanId, int prosperity);

    /** Forgets {@code clanId}, for a clan that no longer exists. */
    @NotNull CompletableFuture<Void> delete(long clanId);

    /** The {@code limit} most prosperous camps, highest first. May block: leaderboards call it off the main thread. */
    @NotNull LinkedHashMap<Long, Integer> top(int limit);

    /** {@code clanId}'s last recorded Prosperity, if it has one. May block, like {@link #top}. */
    @NotNull OptionalInt find(long clanId);
}
