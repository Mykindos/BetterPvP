package me.mykindos.betterpvp.clans.clans.leaderboard;

import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.menus.ClanMenu;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ClanLeaderboard extends Leaderboard<UUID, Clan> implements Sorted {

    private final ClanManager clanManager;

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
    public Description describe(SearchOptions searchOptions, LeaderboardEntry<UUID, Clan> entry) {
        final Clan clan = entry.getValue();
        final Description.DescriptionBuilder builder = Description.builder();
        final ItemStack banner = clan.getBanner().get();
        banner.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ATTRIBUTES);
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
        return builder.properties(map).build();
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
    protected LeaderboardEntry<UUID, Clan> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix) throws UnsupportedOperationException {
        final Optional<Clan> clan = clanManager.getClanByPlayer(player);
        return clan.map(value -> LeaderboardEntry.of(value.getId(), value)).orElse(null);
    }

    @Override
    protected Clan fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix, @NotNull UUID entry) {
        return clanManager.getClanById(entry).orElseThrow();
    }

    @Override
    protected Map<UUID, Clan> fetchAll(@NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix) {
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
