package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.CombatSort;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;

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

import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_TOP_DEATHS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_TOP_HIGHEST_KILLSTREAK;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_TOP_KDR;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_TOP_KILLS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_TOP_KILLSTREAK;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_TOP_RATING;


@Singleton
@CustomLog
public final class GlobalCombatLeaderboard extends PlayerLeaderboard<CombatData> implements Sorted {

    private final GlobalCombatStatsRepository repository;
    private final Database database;

    @Inject
    public GlobalCombatLeaderboard(Core core, GlobalCombatStatsRepository repository, Database database) {
        super(core);
        this.repository = repository;
        this.database = database;
        init();
    }

    @Override
    public String getName() {
        return "Combat";
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.DIAMOND_SWORD)
                        .displayName(Component.text("Combat", NamedTextColor.RED))
                        .build())
                .build();
    }

    @Override
    public LeaderboardCategory getCategory() {
        return LeaderboardCategory.CHAMPIONS;
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
        Table<?> proc = getStatement((CombatSort) sortType);

        try {
            Result<?> fetch = database.getDslContext().selectFrom(proc).fetch();
            fetch.forEach(combatRecord -> {
                final UUID gamer = UUID.fromString(combatRecord.get(0, String.class));
                // We can join this because fetchAll is run on a separate thread
                final CombatData data = repository.getDataAsync(gamer).join();
                map.put(gamer, data);
            });
        } catch (DataAccessException ex) {
            log.error("Failed to load combat rating leaderboard for type " + sortType, ex).submit();
        }

        return map;
    }

    private @NotNull Table<?> getStatement(CombatSort sortType) {
        final int currentRealm = Core.getCurrentRealm().getId();
        final int topResults = 10;

        return switch (sortType) {
            case RATING -> GET_TOP_RATING(currentRealm, topResults);
            case KILLS -> GET_TOP_KILLS(currentRealm, topResults);
            case DEATHS -> GET_TOP_DEATHS(currentRealm, topResults);
            case KDR -> GET_TOP_KDR(currentRealm, topResults);
            case KILLSTREAK -> GET_TOP_KILLSTREAK(currentRealm, topResults);
            case HIGHEST_KILLSTREAK -> GET_TOP_HIGHEST_KILLSTREAK(currentRealm, topResults);

        };
    }


}
