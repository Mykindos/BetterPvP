package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;

/** Prosperity kept in memory. The snapshot is the first value saved for a clan. */
class FakeProsperityStore implements ProsperityStore {

    private final Map<Long, ProsperityStanding> standings = new HashMap<>();

    @Override
    public @NotNull CompletableFuture<Void> save(long clanId, int prosperity) {
        final ProsperityStanding old = standings.get(clanId);
        standings.put(clanId, new ProsperityStanding(prosperity, old == null ? prosperity : old.getSnapshot()));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull CompletableFuture<Void> delete(long clanId) {
        standings.remove(clanId);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public @NotNull LinkedHashMap<Long, Integer> top(int limit) {
        final LinkedHashMap<Long, Integer> top = new LinkedHashMap<>();
        standings.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().getProsperity(), a.getValue().getProsperity()))
                .limit(limit)
                .forEach(entry -> top.put(entry.getKey(), entry.getValue().getProsperity()));
        return top;
    }

    @Override
    public @NotNull OptionalInt find(long clanId) {
        final ProsperityStanding standing = standings.get(clanId);
        return standing == null ? OptionalInt.empty() : OptionalInt.of(standing.getProsperity());
    }

    @Override
    public @NotNull Map<Long, ProsperityStanding> standings() {
        return Map.copyOf(standings);
    }

    @NotNull Map<Long, Integer> prosperity() {
        final Map<Long, Integer> prosperity = new HashMap<>();
        standings.forEach((clan, standing) -> prosperity.put(clan, standing.getProsperity()));
        return prosperity;
    }
}
