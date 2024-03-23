package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.CombatSort;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
@CustomLog
public final class GlobalCombatLeaderboard extends PlayerLeaderboard<CombatData> implements Sorted {

    private static final Map<SortType, Statement> TOP_SORT_STATEMENTS = ImmutableMap.of(
            CombatSort.RATING, new Statement("CALL GetTopRating(?)", new IntegerStatementValue(10)),
            CombatSort.KILLS, new Statement("CALL GetTopKills(?)", new IntegerStatementValue(10)),
            CombatSort.DEATHS, new Statement("CALL GetTopDeaths(?)", new IntegerStatementValue(10)),
            CombatSort.KDR, new Statement("CALL GetTopKDR(?)", new IntegerStatementValue(10)),
            CombatSort.KILLSTREAK, new Statement("CALL GetTopKillstreak(?)", new IntegerStatementValue(10)),
            CombatSort.HIGHEST_KILLSTREAK, new Statement("CALL GetTopHighestKillstreak(?)", new IntegerStatementValue(10))
    );

    private final GlobalCombatStatsRepository repository;

    @Inject
    public GlobalCombatLeaderboard(Core core, GlobalCombatStatsRepository repository) {
        super(core);
        this.repository = repository;
        init();
    }

    @Override
    public String getName() {
        return "Combat";
    }

    @Override
    public Comparator<CombatData> getSorter(SearchOptions searchOptions) {
        final CombatSort sort = (CombatSort) Objects.requireNonNull(searchOptions.getSort());
        return Comparator.comparing((Function<CombatData, Float>) (data -> switch (sort) {
            case RATING -> (float) data.getRating();
            case KILLS -> (float) data.getKills();
            case DEATHS -> (float) data.getDeaths();
            case KDR -> data.getKillDeathRatio();
            case KILLSTREAK -> (float) data.getKillStreak();
            case HIGHEST_KILLSTREAK -> (float) data.getHighestKillStreak();
        })).reversed();
    }

    @NotNull
    @Override
    public SortType[] acceptedSortTypes() {
        return CombatSort.values();
    }

    @Override
    public Map<String, Component> describe(SearchOptions searchOptions, CombatData value) {
        final List<CombatSort> types = new ArrayList<>(Arrays.stream(CombatSort.values()).toList());
        final CombatSort selected = (CombatSort) Objects.requireNonNull(searchOptions.getSort());
        final LinkedHashMap<String, Component> map = new LinkedHashMap<>(); // Preserve order
        types.remove(selected);
        types.add(0, selected);
        for (CombatSort sort : types) {
            final String text = sort.getValue(value);
            final NamedTextColor color = sort == selected ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            map.put(sort.getName(), Component.text(text, color));
        }
        return map;
    }

    @Override
    public final CompletableFuture<Map<SearchOptions, Integer>> add(@NotNull UUID entryName, @NotNull CombatData add) {
        throw new UnsupportedOperationException("Cannot add combat data, only compute/replace");
    }

    @Override
    protected final CombatData join(CombatData value, CombatData add) {
        throw new UnsupportedOperationException("Cannot join combat data, only compute/replace");
    }

    @Override
    protected CombatData fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        // We can join this because fetch is run on a separate thread
        return repository.getDataAsync(entry).join();
    }

    @Override
    protected Map<UUID, CombatData> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        Map<UUID, CombatData> map = new HashMap<>();
        final SortType sortType = Objects.requireNonNull(options.getSort());
        Statement stmt = TOP_SORT_STATEMENTS.get(sortType);
        database.executeProcedure(stmt, -1, result -> {
            try {
                while (result.next()) {
                    final UUID gamer = UUID.fromString(result.getString(1));
                    // We can join this because fetchAll is run on a separate thread
                    final CombatData data = repository.getDataAsync(gamer).join();
                    map.put(gamer, data);
                }
            } catch (SQLException e) {
                log.error("Failed to load combat rating leaderboard for type " + sortType, e);
            }
        });
        return map;
    }

}
