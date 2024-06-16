package me.mykindos.betterpvp.progression.profession.woodcutting.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@CustomLog
@Singleton
public class TotalLogsChoppedLeaderboard extends PlayerLeaderboard<Long> implements Sorted {
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public TotalLogsChoppedLeaderboard(Progression progression, WoodcuttingHandler woodcuttingHandler) {
        super(progression);
        this.woodcuttingHandler = woodcuttingHandler;
        init();
    }

    @Override
    public String getName() {
        return "Total Logs Chopped";
    }

    @Override
    public LeaderboardCategory getCategory() {
        return LeaderboardCategory.PROFESSION;
    }

    @Override
    public Comparator<Long> getSorter(SearchOptions searchOptions) {
        return Comparator.comparing(Long::intValue).reversed();
    }

    /**
     * There's only 1 kind of way you can sort so this is just boilerplate
     */
    @Override
    public SortType[] acceptedSortTypes() {
        return TemporalSort.values();
    }

    /**
     * Pretty sure this method is like some weird functional way to implement
     * a reduce function but since this Leaderboard only sorts actual numbers,
     * it can be this basic
     */
    @Override
    protected Long join(Long value, Long add) {
        return value + add;
    }

    @Override
    @SneakyThrows
    protected Long fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        return woodcuttingHandler.getWoodcuttingRepository().getTotalChoppedLogsForPlayer(entry);
    }

    @Override
    @SneakyThrows
    protected Map<UUID, Long> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        final TemporalSort type = (TemporalSort) Objects.requireNonNull(options.getSort());
        return woodcuttingHandler.getWoodcuttingRepository().getTopLogsChoppedByCount(type.getDays()).join();
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.IRON_AXE)
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .displayName(Component.text("Total Chopped Logs", NamedTextColor.LIGHT_PURPLE))
                        .build())
                .build();
    }
}
