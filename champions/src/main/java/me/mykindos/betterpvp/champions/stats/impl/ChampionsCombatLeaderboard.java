package me.mykindos.betterpvp.champions.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsStatsRepository;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatLeaderboard;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.CombatSort;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.filter.Filtered;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Singleton
@CustomLog
public final class ChampionsCombatLeaderboard extends PlayerLeaderboard<CombatData> implements Sorted, Filtered {

    private final ChampionsStatsRepository repository;
    private final GlobalCombatLeaderboard globalLeaderboard;

    @Inject
    public ChampionsCombatLeaderboard(Core core, ChampionsStatsRepository repository, GlobalCombatLeaderboard globalLeaderboard) {
        super(core);
        this.repository = repository;
        this.globalLeaderboard = globalLeaderboard;
        globalLeaderboard.setViewable(false); // Disable to prevent conflicts
        init();
    }

    @Override
    public String getName() {
        return "Combat";
    }

    @Override
    public SortedSet<LeaderboardEntry<UUID, CombatData>> getTopTen(SearchOptions options) {
        final ChampionsFilter filter = (ChampionsFilter) Objects.requireNonNull(options.getFilter());
        if (filter == ChampionsFilter.GLOBAL) {
            final CombatSort sortType = (CombatSort) Objects.requireNonNull(options.getSort());
            final SearchOptions globalOptions = SearchOptions.builder().sort(sortType).build();
            return globalLeaderboard.getTopTen(globalOptions);
        }
        return super.getTopTen(options);
    }

    @Override
    public CompletableFuture<CombatData> getEntryData(SearchOptions searchOptions, UUID entry) {
        final ChampionsFilter filter = (ChampionsFilter) Objects.requireNonNull(searchOptions.getFilter());
        if (filter == ChampionsFilter.GLOBAL) {
            final CombatSort sortType = (CombatSort) Objects.requireNonNull(searchOptions.getSort());
            final SearchOptions globalOptions = SearchOptions.builder().sort(sortType).build();
            return globalLeaderboard.getEntryData(globalOptions, entry);
        }
        return super.getEntryData(searchOptions, entry);
    }

    @NotNull
    @Override
    public FilterType @NotNull [] acceptedFilters() {
        return ChampionsFilter.values();
    }

    @Override
    public SortType[] acceptedSortTypes() {
        return globalLeaderboard.acceptedSortTypes();
    }

    @Override
    public Comparator<CombatData> getSorter(SearchOptions searchOptions) {
        return globalLeaderboard.getSorter(searchOptions);
    }

    @Override
    protected CombatData join(CombatData value, CombatData add) {
        throw new UnsupportedOperationException("Cannot join combat data, only compute/replace");
    }

    @Override
    public Map<String, Component> describe(SearchOptions searchOptions, CombatData value) {
        return globalLeaderboard.describe(searchOptions, value);
    }

    @Override
    public CompletableFuture<Map<SearchOptions, Integer>> add(@NotNull UUID entryName, @NotNull CombatData add) {
        throw new UnsupportedOperationException("Cannot add combat data, only compute/replace");
    }

    @Override
    protected CombatData fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        final ChampionsFilter filter = (ChampionsFilter) Objects.requireNonNull(options.getFilter());
        if (filter == ChampionsFilter.GLOBAL) {
            final CombatSort sortType = (CombatSort) Objects.requireNonNull(options.getSort());
            final SearchOptions globalOptions = SearchOptions.builder().sort(sortType).build();
            return globalLeaderboard.getEntryData(globalOptions, entry).join();
        }

        return repository.fetchDataAsync(entry).join().getCombatData(filter);
    }

    @Override
    protected Map<UUID, CombatData> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        final ChampionsFilter filter = (ChampionsFilter) Objects.requireNonNull(options.getFilter());
        final Map<UUID, CombatData> map = new HashMap<>();
        final CombatSort sortType = (CombatSort) Objects.requireNonNull(options.getSort());
        if (filter == ChampionsFilter.GLOBAL) {
            final SearchOptions globalOptions = SearchOptions.builder().sort(sortType).build();
            final SortedSet<LeaderboardEntry<UUID, CombatData>> topTen = globalLeaderboard.getTopTen(globalOptions);
            topTen.forEach(entry -> map.put(entry.getKey(), entry.getValue()));
            return map;
        }

        final String className = Optional.ofNullable(filter.getRole()).map(Role::getName).orElse("");
        final IntegerStatementValue top = new IntegerStatementValue(10);
        final StringStatementValue classNameStmt = new StringStatementValue(className);
        Statement stmt = switch (sortType) {
            case RATING -> new Statement("CALL GetTopRatingByClass(?, ?);", top, classNameStmt);
            case KILLS -> new Statement("CALL GetTopKillsByClass(?, ?);", top, classNameStmt);
            case KDR -> new Statement("CALL GetTopKDRByClass(?, ?);", top, classNameStmt);
            case KILLSTREAK -> new Statement("CALL GetTopKillstreakByClass(?, ?);", top, classNameStmt);
            case HIGHEST_KILLSTREAK -> new Statement("CALL GetTopHighestKillstreakByClass(?, ?);", top, classNameStmt);
            case DEATHS -> new Statement("CALL GetTopDeathsByClass(?, ?);", top, classNameStmt);
        };

        database.executeProcedure(stmt, -1, result -> {
            try {
                while (result.next()) {
                    final UUID gamer = UUID.fromString(result.getString(1));
                    // We can join this because fetchAll is run on a separate thread
                    final CombatData data = repository.fetchDataAsync(gamer).join().getCombatData(filter);
                    map.put(gamer, data);
                }
            } catch (SQLException e) {
                log.error("Failed to load combat rating leaderboard for type " + sortType, e);
            }
        });
        return map;
    }
}
