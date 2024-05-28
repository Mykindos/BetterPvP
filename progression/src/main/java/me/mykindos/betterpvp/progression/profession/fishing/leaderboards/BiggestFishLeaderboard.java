package me.mykindos.betterpvp.progression.profession.fishing.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.stats.sort.SortType;
import me.mykindos.betterpvp.core.stats.sort.Sorted;
import me.mykindos.betterpvp.core.stats.sort.TemporalSort;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.fishing.data.CaughtFish;
import me.mykindos.betterpvp.progression.profession.fishing.repository.FishingRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
@Singleton
public class BiggestFishLeaderboard extends Leaderboard<UUID, CaughtFish> implements Sorted {

    private final ClientManager clientManager;
    private final FishingRepository fishingRepository;

    @Inject
    public BiggestFishLeaderboard(Progression progression, ClientManager clientManager, FishingRepository fishingRepository) {
        super(progression);
        this.clientManager = clientManager;
        this.fishingRepository = fishingRepository;
        init();
    }

    @Override
    public String getName() {
        return "Biggest Fish Caught";
    }

    @Override
    public Comparator<CaughtFish> getSorter(SearchOptions searchOptions) {
        return Comparator.comparing(CaughtFish::getWeight).reversed();
    }

    @Override
    protected CaughtFish join(CaughtFish value, CaughtFish add) {
        return value.getWeight() > add.getWeight() ? value : add;
    }

    @Override
    protected LeaderboardEntry<UUID, CaughtFish> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options, @NotNull Database database) throws UnsupportedOperationException {
        return LeaderboardEntry.of(player, fetch(options, database, player));
    }


    @Override
    public SortType[] acceptedSortTypes() {
        return TemporalSort.values();
    }


    @Override
    protected CaughtFish fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        final TemporalSort type = (TemporalSort) Objects.requireNonNull(options.getSort());
        return fishingRepository.getBiggestFishForGamer(entry, type.getDays()).join();
    }

    @SneakyThrows
    @Override
    protected Map<UUID, CaughtFish> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        final TemporalSort type = (TemporalSort) Objects.requireNonNull(options.getSort());
        return fishingRepository.getBiggestFishOverall(type.getDays()).join();
    }

    @Override
    protected CompletableFuture<Description> describe(SearchOptions searchOptions, LeaderboardEntry<UUID, CaughtFish> value) {

        final CompletableFuture<Description> future = new CompletableFuture<>();
        if (value.getValue() == null) {
            future.complete(null);
            return future;
        }


        final OfflinePlayer player = Bukkit.getOfflinePlayer(value.getValue().getGamer());

        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
        meta.setPlayerProfile(player.getPlayerProfile());
        itemStack.setItemMeta(meta);

        // Update name when loaded
        this.clientManager.search().offline(player.getUniqueId(), clientOpt -> {
            final Map<String, Component> result = new LinkedHashMap<>();
            result.put("Player", Component.text(clientOpt.map(Client::getName).orElse(player.getUniqueId().toString())));
            CaughtFish caughtFish = value.getValue();
            result.put("Biggest Fish Caught", Component.text(UtilFormat.formatNumber(caughtFish.getWeight()) + "lb " + caughtFish.getType()));


            final Description description = Description.builder()
                    .icon(itemStack)
                    .properties(result)
                    .build();
            future.complete(description);
        });

        return future;
    }

}
