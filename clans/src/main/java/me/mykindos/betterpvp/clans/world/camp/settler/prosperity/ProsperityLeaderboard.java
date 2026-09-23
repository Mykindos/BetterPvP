package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** The most prosperous camps, read from the {@link ProsperityStore} so camps that are not loaded rank too. */
public class ProsperityLeaderboard extends Leaderboard<Long, Integer> {

    private final ClanManager clanManager;
    private final ProsperityStore store;

    @Inject
    public ProsperityLeaderboard(@NotNull Clans clans, @NotNull ClanManager clanManager,
                                 @NotNull ProsperityStore store) {
        super(clans);
        this.clanManager = clanManager;
        this.store = store;
        init();
    }

    @Override
    public String getName() {
        return "Prosperity";
    }

    @Override
    public LeaderboardCategory getCategory() {
        return LeaderboardCategory.CLANS;
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.BELL)
                        .displayName(Translations.component("clans.camp.prosperity.leaderboard").color(NamedTextColor.GOLD))
                        .build())
                .build();
    }

    @Override
    public Comparator<Integer> getSorter(SearchOptions searchOptions) {
        return Comparator.<Integer>naturalOrder().reversed();
    }

    @Override
    protected CompletableFuture<Description> describe(SearchOptions searchOptions, LeaderboardEntry<Long, Integer> entry) {
        final Component name = clanManager.getClanById(entry.getKey())
                .<Component>map(clan -> Component.text(clan.getName(), NamedTextColor.YELLOW))
                .orElseGet(() -> Translations.component("clans.camp.zone.unnamed").color(NamedTextColor.GRAY));
        return CompletableFuture.completedFuture(Description.builder()
                .icon(ItemView.builder()
                        .material(Material.BELL)
                        .displayName(name)
                        .lore(Translations.component("clans.camp.prosperity.value",
                                Component.text(entry.getValue(), NamedTextColor.GOLD)).color(NamedTextColor.GRAY))
                        .build())
                .build());
    }

    @Override
    protected Integer join(Integer value, Integer add) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Map<SearchOptions, Integer>> add(@NotNull Long entryName, @NotNull Integer add) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected LeaderboardEntry<Long, Integer> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options,
                                                             @NotNull Database database) {
        final Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if (clan == null) {
            return null;
        }
        final OptionalInt found = store.find(clan.getId());
        return found.isPresent() ? LeaderboardEntry.of(clan.getId(), found.getAsInt()) : null;
    }

    @Override
    protected Integer fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull Long entry) {
        return store.find(entry).orElse(0);
    }

    @Override
    protected Map<Long, Integer> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        return store.top(10);
    }
}
