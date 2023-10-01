package me.mykindos.betterpvp.core.utilities.model.leaderboard;

import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
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
public abstract class Leaderboard<E, T> {

    private final ConcurrentHashMap<SortType, TreeSet<LeaderboardEntry<E, T>>> topTen;

    protected Leaderboard(BPvPPlugin plugin, String tablePrefix) {
        final Database database = plugin.getInjector().getInstance(Database.class);
        this.topTen = new ConcurrentHashMap<>();

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            for (SortType sortType : acceptedSortTypes()) {
                CompletableFuture.supplyAsync(() -> fetch(sortType, database, tablePrefix)).thenApply(fetch -> {
                    TreeSet<LeaderboardEntry<E, T>> set = new TreeSet<>(Comparator.comparing(LeaderboardEntry::getValue, getSorter()));
                    set.addAll(fetch.entrySet().stream().map(entry -> LeaderboardEntry.of(entry.getKey(), entry.getValue())).toList());
                    return set;
                }).exceptionally( (ex) -> {
                    ex.printStackTrace();
                    return null;
                }).whenComplete((set, ex) -> {
                    if (ex != null) {
                        ex.printStackTrace();
                        return;
                    }
                    topTen.put(sortType, set);
                });
            }
        }, 0L, 10L, TimeUnit.MINUTES);
    }

    public abstract String getName();

    /**
     * @return The comparator to sort the leaderboard by.
     */
    protected abstract Comparator<T> getSorter();

    /**
     * @return The types of sorting this leaderboard accepts.
     */
    protected abstract SortType[] acceptedSortTypes();

    /**
     * @return The top entries in the leaderboard of type T.
     */
    public final SortedSet<LeaderboardEntry<E, T>> getTopTen(SortType sortType) {
        if (!Arrays.asList(acceptedSortTypes()).contains(sortType)) {
            throw new IllegalArgumentException("Sort type " + sortType + " is not accepted by this leaderboard.");
        }
        return Collections.unmodifiableSortedSet(topTen.get(sortType));
    }

    /**
     * Attempts to add element to all <b>loaded</b> leaderboards.
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
     * Fetches the top entries from the database.
     * @param sortType The type of sorting to use.
     * @param database The database to fetch from.
     * @param tablePrefix The prefix of the table.
     */
    protected abstract Map<E, T> fetch(@NotNull SortType sortType, @NotNull Database database, @NotNull String tablePrefix);


}
