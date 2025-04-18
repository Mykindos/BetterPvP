package me.mykindos.betterpvp.clans.clans.leaderboard;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClanLeaderboard extends Leaderboard<UUID, Clan> implements Sorted {

    private final ClanManager clanManager;

    @Inject
    public ClanLeaderboard(Clans clans, ClanManager clanManager) {
        super(clans);
        this.clanManager = clanManager;
        init();
    }

    @Override
    public String getName() {
        return "Clans";
    }

    @Override
    public LeaderboardCategory getCategory() {
        return LeaderboardCategory.CLANS;
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.IRON_DOOR)
                        .displayName(Component.text("Clans", NamedTextColor.DARK_RED))
                        .build())
                .build();
    }

    @Override
    public Comparator<Clan> getSorter(SearchOptions searchOptions) {
        final ClanSort sort = (ClanSort) Objects.requireNonNull(searchOptions.getSort());
        return switch (sort) {
            case LEVEL:
                yield Comparator.comparingLong(Clan::getLevel).reversed();
            case BALANCE:
                yield Comparator.comparingLong(Clan::getBalance).reversed();
            case POINTS:
                yield Comparator.comparingLong(Clan::getPoints).reversed();
        };
    }

    @Override
    public CompletableFuture<Description> describe(SearchOptions searchOptions, LeaderboardEntry<UUID, Clan> entry) {
        final Clan clan = entry.getValue();
        final Description.DescriptionBuilder builder = Description.builder();
        final ItemStack banner = clan.getBanner().get();
        banner.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP, ItemFlag.HIDE_ATTRIBUTES);
        builder.icon(banner);
        builder.property("Clan", Component.text(clan.getName()));
        builder.clickFunction(click -> {
            final Player player = click.getPlayer();
            final Clan selfClan = clanManager.getClanByPlayer(player).orElse(null);
            new ClanMenu(player, selfClan, clan).show(player);
        });

        final List<ClanSort> types = new ArrayList<>(Arrays.stream(ClanSort.values()).toList());
        final ClanSort selected = (ClanSort) Objects.requireNonNull(searchOptions.getSort());
        final LinkedHashMap<String, Component> map = new LinkedHashMap<>(); // Preserve order
        types.remove(selected);
        types.add(0, selected);
        for (ClanSort sort : types) {
            final String text = sort.getValue(clan);
            final NamedTextColor color = sort == selected ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            map.put(sort.getName(), Component.text(text, color));
        }
        return CompletableFuture.completedFuture(builder.properties(map).build());
    }

    @Override
    protected Clan join(Clan value, Clan add) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Map<SearchOptions, Integer>> add(@NotNull UUID entryName, @NotNull Clan add) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected LeaderboardEntry<UUID, Clan> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options, @NotNull Database database) throws UnsupportedOperationException {
        final Optional<Clan> clan = clanManager.getClanByPlayer(player);
        return clan.map(value -> LeaderboardEntry.of(value.getId(), value)).orElse(null);
    }

    @Override
    protected Clan fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        return clanManager.getClanById(entry).orElseThrow();
    }

    @Override
    protected Map<UUID, Clan> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        final ClanSort sort = (ClanSort) Objects.requireNonNull(options.getSort());
        final Collection<Clan> pool = new HashSet<>(clanManager.getObjects().values());
        pool.removeIf(Clan::isAdmin);
        return switch (sort) {
            case LEVEL -> pool.stream()
                    .sorted(Comparator.comparingLong(Clan::getLevel).reversed())
                    .limit(10)
                    .collect(Collectors.toMap(Clan::getId, Function.identity()));
            case BALANCE -> pool.stream()
                    .sorted(Comparator.comparingLong(Clan::getBalance).reversed())
                    .limit(10)
                    .collect(Collectors.toMap(Clan::getId, Function.identity()));
            case POINTS -> pool.stream()
                    .sorted(Comparator.comparingLong(Clan::getPoints).reversed())
                    .limit(10)
                    .collect(Collectors.toMap(Clan::getId, Function.identity()));
        };
    }

    @Override
    public SortType[] acceptedSortTypes() {
        return ClanSort.values();
    }
}
