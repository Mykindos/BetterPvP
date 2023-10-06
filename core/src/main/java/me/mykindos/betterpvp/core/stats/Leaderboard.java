package me.mykindos.betterpvp.core.stats;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.stats.event.LeaderboardInitializeEvent;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntryComparator;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntryKey;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Represents a sorted leaderboard for objects of type T.
 * <p>
 * Currently, Leaderboards only support up to min-max of 10 entries only.
 * @param <T> The type of object to be sorted in this leaderboard.
 */
@Slf4j
public abstract class Leaderboard<E, T extends Comparable<T>> {

    private final ConcurrentHashMap<SortType, TreeSet<LeaderboardEntry<E, T>>> topTen;
    private final AsyncLoadingCache<LeaderboardEntryKey<E>, T> entryCache;

    protected Leaderboard(BPvPPlugin plugin, String tablePrefix) {
        Validate.isTrue(acceptedSortTypes().length > 0, "Leaderboard must accept at least one sort type.");
        final Database database = plugin.getInjector().getInstance(Database.class);
        this.topTen = new ConcurrentHashMap<>();
        this.entryCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> fetch(key.getSortType(), database, tablePrefix, key.getValue())));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (SortType sortType : acceptedSortTypes()) {
                CompletableFuture.supplyAsync(() -> fetchAll(sortType, database, tablePrefix)).thenApply(fetch -> {
                    TreeSet<LeaderboardEntry<E, T>> set = new TreeSet<>(new LeaderboardEntryComparator<>());
                    set.addAll(fetch.entrySet().stream().map(entry -> LeaderboardEntry.of(entry.getKey(), entry.getValue())).toList());
                    return set;
                }).exceptionally(ex -> {
                    log.error("Failed to fetch leaderboard data for " + sortType + "!", ex);
                    ex.printStackTrace();
                    return null;
                }).whenComplete((set, ex) -> {
                    if (ex != null) {
                        log.error("Failed to fetch leaderboard data for " + sortType + "!", ex);
                        ex.printStackTrace();
                        return;
                    }
                    topTen.put(sortType, set);
                });
            }
        }, 0L, 10L, TimeUnit.MINUTES);

        UtilServer.callEvent(new LeaderboardInitializeEvent(this));
    }

    public abstract String getName();

    /**
     * @return The comparator to sort the leaderboard by.
     */
    protected abstract Comparator<T> getSorter();

    /**
     * @return The types of sorting this leaderboard accepts.
     */
    public abstract SortType[] acceptedSortTypes();

    /**
     * @return The top entries in the leaderboard of type T.
     */
    public final SortedSet<LeaderboardEntry<E, T>> getTopTen(SortType sortType) {
        if (!Arrays.asList(acceptedSortTypes()).contains(sortType)) {
            log.error("Sort type " + sortType + " is not accepted by this leaderboard.");
            throw new IllegalArgumentException("Sort type " + sortType + " is not accepted by this leaderboard.");
        }
        return Collections.unmodifiableSortedSet(topTen.get(sortType));
    }

    /**
     * Attempts to add element to all <b>loaded</b> leaderboards.
     * This methods will return a {@link CompletableFuture} because if a player is not already loaded
     * in the leaderboard, it will have to be loaded from the database to see if it should be added.
     *
     * @param entryName The name of the entry.
     * @param add The element to add.
     * @return The types of leaderboards the element was replaced into mapped to its 1-based index within it.
     *        If the element was not added to a leaderboard, it will not be present in the map.
     *        If the element was already in the same position, it will not be present in the map.
     */
    public final CompletableFuture<Map<SortType, Integer>> add(@NotNull E entryName, @NotNull T add) {
        return CompletableFuture.supplyAsync(() -> {
            Map<SortType, Integer> types = new HashMap<>();
            for (SortType type : acceptedSortTypes()) {
                final TreeSet<LeaderboardEntry<E, T>> set = topTen.get(type);
                if (set == null) {
                    continue;
                }

                final ArrayList<LeaderboardEntry<E, T>> list = new ArrayList<>(set);
                LeaderboardEntry<E, T> entry = LeaderboardEntry.of(entryName, add);
                final T existingData;
                final Optional<LeaderboardEntry<E, T>> match = set.stream().filter(e -> e.getKey().equals(entry.getKey())).findFirst();
                if (match.isPresent()) {
                    existingData = match.orElseThrow().getValue();
                } else {
                    existingData = entryCache.get(LeaderboardEntryKey.of(type, entryName)).join(); // Reason for this to be async
                }

                entry.setValue(join(existingData, add));
                int indexBefore = list.contains(entry) ? list.indexOf(entry) + 1 : -1;
                set.removeIf(existing -> existing.getKey().equals(entry.getKey())); // Remove entry if cloned
                set.add(entry);
                if (set.size() > 10) {
                    set.pollLast(); // Remove last entry to keep the same size, only if we updated the size
                }

                // Only return this type if the entry was added
                var newList = new ArrayList<>(set);

                int indexNow = newList.indexOf(entry) + 1;
                if (set.contains(entry) && indexBefore != indexNow) {
                    types.put(type, indexNow);
                }
            }
            return types;
        }).exceptionally(ex -> {
            log.error("Failed to add " + entryName + " to leaderboard!", ex);
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Joins two values together. Usually is an additive function for linear leaderboardsa.
     * For more complex types, you would want to add a special implementation.
     * @param value The value to join to.
     * @param add The value to add.
     * @return The joined value.
     */
    protected abstract T join(T value, T add);

    /**
     * Attempts to put an element to all <b>loaded</b> leaderboards.
     * @param entryName The name of the entry.
     * @param element The element to add.
     * @return The types of leaderboards the element was replaced into mapped to its 1-based index within it.
     *         If the element was not added to a leaderboard, it will not be present in the map.
     *         If the element was already in the same position, it will not be present in the map.
     */
    public final Map<SortType, Integer> compute(@NotNull E entryName, @NotNull T element) {
        Map<SortType, Integer> types = new HashMap<>();
        for (SortType type : acceptedSortTypes()) {
            final TreeSet<LeaderboardEntry<E, T>> set = topTen.get(type);
            if (set == null) {
                continue;
            }

            final ArrayList<LeaderboardEntry<E, T>> list = new ArrayList<>(set);
            LeaderboardEntry<E, T> entry = LeaderboardEntry.of(entryName, element);
            int indexBefore = list.contains(entry) ? list.indexOf(entry) + 1 : -1;
            set.removeIf(existing -> existing.getKey().equals(entry.getKey())); // Remove entry if cloned
            set.add(entry);
            if (set.size() > 10) {
                set.pollLast(); // Remove last entry to keep the same size, only if we updated the size
            }

            // Only return this type if the entry was added
            int indexNow = new ArrayList<>(set).indexOf(entry) + 1;
            if (set.contains(entry) && indexBefore != indexNow) {
                types.put(type, indexNow);
            }
        }
        return types;
    }

    /**
     * Gets the data in this leaderboard for the given entry.
     * @param entry The entry to get the data for.
     * @return The data in this leaderboard for the given entry.
     */
    public final CompletableFuture<T> getEntryData(SortType sortType, E entry) {
        return topTen.get(sortType).stream()
                .filter(e -> e.getKey().equals(entry))
                .findFirst()
                .map(LeaderboardEntry::getValue)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> entryCache.get(LeaderboardEntryKey.of(sortType, entry)));
    }

    /**
     * Loads the data for the given entry.
     * @param sortType The type of sorting to use.
     * @param database The database to fetch from.
     * @param tablePrefix The prefix of the table.
     * @param entry The entry to load the data for.
     * @return The data for the given entry.
     */
    protected abstract T fetch(SortType sortType, @NotNull Database database, @NotNull String tablePrefix, @NotNull E entry);

    /**
     * Fetches the top entries from the database.
     * @param sortType The type of sorting to use.
     * @param database The database to fetch from.
     * @param tablePrefix The prefix of the table.
     */
    protected abstract Map<E, T> fetchAll(@NotNull SortType sortType, @NotNull Database database, @NotNull String tablePrefix);

}
