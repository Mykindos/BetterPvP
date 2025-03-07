package me.mykindos.betterpvp.core.stats;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.BPvPPlugin;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.filter.Filtered;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntryComparator;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntryKey;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilSound;
import me.mykindos.betterpvp.core.utilities.model.description.Describable;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Represents a sorted leaderboard for objects of type T.
 * <p>
 * Currently, Leaderboards only support up to min-max of 10 entries only.
 * @param <T> The type of object to be sorted in this leaderboard.
 */
@CustomLog
public abstract class Leaderboard<E, T> implements Describable {

    private static final ExecutorService LEADERBOARD_UPDATER = Executors.newSingleThreadExecutor();

    private final ConcurrentHashMap<SearchOptions, ConcurrentSkipListSet<LeaderboardEntry<E, T>>> topTen;
    private final AsyncLoadingCache<LeaderboardEntryKey<E>, T> entryCache;
    private final Database database;
    private final Collection<SearchOptions> validSearchOptions = new ArrayList<>();
    private static int INITIAL_DELAY = 0;
    private static int DELAY_BETWEEN_UPDATE = 0;
    private boolean loaded = false;

    @Getter
    @Setter
    private boolean viewable = true;

    protected Leaderboard(BPvPPlugin plugin) {
        this.database = plugin.getInjector().getInstance(Database.class);
        this.topTen = new ConcurrentHashMap<>();
        this.entryCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .buildAsync((key, executor) -> CompletableFuture.supplyAsync(() -> fetch(key.getOptions(), database, key.getValue()), LEADERBOARD_UPDATER));
    }

    protected void init() {
        // Populate search options
        if (this instanceof Filtered filtered && this instanceof Sorted sorted) {
            // Both sorts and filters
            for (SortType sortType : sorted.acceptedSortTypes()) {
                for (FilterType filterType : filtered.acceptedFilters()) {
                    validSearchOptions.add(SearchOptions.builder().sort(sortType).filter(filterType).build());
                }
            }
        } else if (this instanceof Filtered filtered) {
            // Empty sorts, only filters
            for (FilterType filterType : filtered.acceptedFilters()) {
                validSearchOptions.add(SearchOptions.builder().filter(filterType).build());
            }
        } else if (this instanceof Sorted sorted) {
            // Empty filters, only sorts
            for (SortType sortType : sorted.acceptedSortTypes()) {
                validSearchOptions.add(SearchOptions.builder().sort(sortType).build());
            }
        }

        // If no sorts or filters, add empty
        if (validSearchOptions.isEmpty()) {
            validSearchOptions.add(SearchOptions.EMPTY);
        }

        // Schedule updates and register with manager
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::forceUpdate, INITIAL_DELAY, 1200L + DELAY_BETWEEN_UPDATE, TimeUnit.SECONDS);
        INITIAL_DELAY += 10;
        DELAY_BETWEEN_UPDATE += 15;
    }

    public void forceUpdate() {
        for (SearchOptions options : validSearchOptions) {
            CompletableFuture.supplyAsync(() -> fetchAll(options, database), LEADERBOARD_UPDATER).thenApply(fetch -> {
                final LeaderboardEntryComparator<E, T> comparator = new LeaderboardEntryComparator<>(getSorter(options));
                ConcurrentSkipListSet<LeaderboardEntry<E, T>> set = new ConcurrentSkipListSet<>(comparator);
                set.addAll(fetch.entrySet().stream().map(entry -> LeaderboardEntry.of(entry.getKey(), entry.getValue())).toList());
                return set;
            }).exceptionally(ex -> {
                log.error("Failed to fetch leaderboard data for " + options + "!", ex).submit();
                return null;
            }).whenComplete((set, ex) -> {
                if (ex != null) {
                    log.error("Failed to fetch leaderboard data for " + options + "!", ex).submit();
                    return;
                }
                topTen.put(options, set);
            });
        }

        loaded = true;
        log.info("Leaderboard " + getName() + " has been updated!").submit();
    }

    private void validate(SearchOptions options) {
        Preconditions.checkNotNull(options, "Search options cannot be null!");
        Preconditions.checkArgument(validSearchOptions.contains(options), "Search options " + options + " are not valid for this leaderboard!");
    }

    public abstract String getName();

    public abstract LeaderboardCategory getCategory();

    /**
     * @return The comparator to sort the leaderboard by.
     */
    public abstract Comparator<T> getSorter(SearchOptions searchOptions);

    /**
     * Gets the description of the given value for a leaderboard menu.
     * @param searchOptions The options to get the description for.
     * @param value The value to get the description for.
     * @return The description of the given value.
     */
    public CompletableFuture<Description> getDescription(SearchOptions searchOptions, LeaderboardEntry<E, T> value) {
        validate(searchOptions);
        return describe(searchOptions, value);
    }

    /**
     * Gets the description of the given value for a leaderboard menu.
     * @param searchOptions The options to get the description for.
     * @param value The value to get the description for.
     * @return The description of the given value.
     */
    protected abstract CompletableFuture<Description> describe(SearchOptions searchOptions, LeaderboardEntry<E, T> value);

    /**
     * @return The top entries in the leaderboard of type T.
     */
    public SortedSet<LeaderboardEntry<E, T>> getTopTen(SearchOptions options) {
        validate(options);
        ConcurrentSkipListSet<LeaderboardEntry<E, T>> leaderboardEntries = topTen.get(options);
        if(leaderboardEntries != null) {
            return Collections.unmodifiableSortedSet(leaderboardEntries);
        }
        return Collections.emptySortedSet();
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
    public CompletableFuture<Map<SearchOptions, Integer>> add(@NotNull E entryName, @NotNull T add) {
        return CompletableFuture.supplyAsync(() -> {
            Map<SearchOptions, Integer> types = new HashMap<>();
            for (SearchOptions options : validSearchOptions) {
                final ConcurrentSkipListSet<LeaderboardEntry<E, T>> set = topTen.get(options);
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
                    existingData = entryCache.get(LeaderboardEntryKey.of(options, entryName)).join(); // Reason for this to be async
                }

                if(existingData != null) {
                    entry.setValue(join(existingData, add));
                }

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
                    types.put(options, indexNow);
                }
            }
            return types;
        }).exceptionally(ex -> {
            log.error("Failed to add " + entryName + " to leaderboard!", ex).submit();
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
    public Map<SearchOptions, Integer> compute(@NotNull E entryName, @NotNull T element) {
        Map<SearchOptions, Integer> types = new HashMap<>();
        for (SearchOptions options : validSearchOptions) {
            if (!options.accepts(element)) {
                continue;
            }

            final ConcurrentSkipListSet<LeaderboardEntry<E, T>> set = topTen.get(options);
            if (set == null) {
                continue;
            }

            final ArrayList<LeaderboardEntry<E, T>> list = new ArrayList<>(set);
            LeaderboardEntry<E, T> entry = LeaderboardEntry.of(entryName, element);
            int indexBefore = list.contains(entry) ? list.indexOf(entry) + 1 : -1;
            set.removeIf(existing -> existing.getKey().equals(entry.getKey())); // Remove entry if cloned
            set.add(entry);
            if (set.size() > 10) {
                set.pollLast(); // Remove the last entry to keep the same size, only if we updated the size
            }

            // Only return this type if the entry was added
            int indexNow = new ArrayList<>(set).indexOf(entry) + 1;
            if (set.contains(entry) && indexBefore != indexNow) {
                types.put(options, indexNow);
            }
        }
        return types;
    }

    /**
     * Gets the data in this leaderboard for the given entry.
     * @param searchOptions The options to load the data for.
     * @param entry The entry to get the data for.
     * @return The data in this leaderboard for the given entry.
     */
    public CompletableFuture<T> getEntryData(SearchOptions searchOptions, E entry) {
        validate(searchOptions);
        return topTen.get(searchOptions).stream()
                .filter(e -> e.getKey().equals(entry))
                .findFirst()
                .map(LeaderboardEntry::getValue)
                .map(CompletableFuture::completedFuture)
                .orElseGet(() -> entryCache.get(LeaderboardEntryKey.of(searchOptions, entry)));
    }

    /**
     * Gets the data in this leaderboard that can correspond to the given player.
     * @param player The player to get the data for.
     * @param options The options to load the data for.
     * @return The data in this leaderboard that can correspond to the given player.
     */
    public final CompletableFuture<Optional<LeaderboardEntry<E, T>>> getPlayerData(@NotNull UUID player, @NotNull SearchOptions options) {
        // Fetch the player data
        CompletableFuture<Optional<LeaderboardEntry<E, T>>> future = CompletableFuture.supplyAsync(() -> Optional.ofNullable(fetchPlayerData(player, options, database))).exceptionally(ex -> {
            if (!(ex instanceof UnsupportedOperationException)) {
                log.error("Failed to fetch leaderboard data for " + player + "!", ex).submit();
            }
            return Optional.empty();
        });

        // Cache the data we just got
        future.thenAccept(data -> {
            if (data.isPresent()) {
                final LeaderboardEntry<E, T> entry = data.get();
                entryCache.put(LeaderboardEntryKey.of(options, entry.getKey()), CompletableFuture.completedFuture(entry.getValue()));
            }
        });
        return future;
    }

    /**
     * Gets the data in this leaderboard that can correspond to the given player.
     *
     * @param player The player to get the data for.
     * @param options The options to load the data for.
     * @param database The database to fetch from.
     * @return The data in this leaderboard that can correspond to the given player.
     */
    protected abstract LeaderboardEntry<E, T> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options, @NotNull Database database) throws UnsupportedOperationException;

    /**
     * Loads the data for the given entry.
     *
     * @param options  The options to load the data for.
     * @param database The database to fetch from.
     * @param entry    The entry to load the data for.
     * @return The data for the given entry.
     */
    protected abstract T fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull E entry);

    /**
     * Fetches the top entries from the database.
     *
     * @param options  The options to load the data for.
     * @param database The database to fetch from.
     */
    protected abstract Map<E, T> fetchAll(@NotNull SearchOptions options, @NotNull Database database);

    public void attemptAnnounce(Player player, Map<SearchOptions, Integer> newPositions) {
        if(!loaded){
            return;
        }

        if (newPositions.isEmpty() || !isViewable()) {
            return; // No new positions or leaderboard is disabled
        }

        final int highestEntry = newPositions.getOrDefault(SearchOptions.builder().sort(TemporalSort.SEASONAL).build(), 0);
        if (highestEntry != 1) {
            return; // We only announce for top 1 of season
        }

        final String playerName = player.getName();
        UtilMessage.simpleBroadcast("Leaderboard", "<dark_green>%s <green>has reached <dark_green>#%d</dark_green> on the seasonal %s leaderboard!",
                playerName,
                highestEntry,
                this.getName());

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            UtilSound.playSound(onlinePlayer, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0F, 1.0F, true);
        }
    }

}
