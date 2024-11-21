package me.mykindos.betterpvp.progression.profession.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.profiles.PlayerProfiles;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.stats.filter.FilterType;
import me.mykindos.betterpvp.core.stats.filter.Filtered;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardEntry;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.ProfessionRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@CustomLog
@Singleton
public class ProfessionLevelLeaderboard extends Leaderboard<UUID, Long> implements Filtered {

    private final ClientManager clientManager;
    private final ProfessionRepository professionRepository;
    private final FilterType[] filters;

    @Inject
    public ProfessionLevelLeaderboard(Progression progression, ClientManager clientManager, ProfessionRepository professionRepository) {
        super(progression);
        this.clientManager = clientManager;
        this.professionRepository = professionRepository;
        this.filters = ProfessionFilter.values();
        init();
    }

    @Override
    public String getName() {
        return "Profession Level";
    }

    @Override
    public LeaderboardCategory getCategory() {
        return LeaderboardCategory.PROFESSION;
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.EXPERIENCE_BOTTLE)
                        .displayName(Component.text("Profession Level", NamedTextColor.GREEN))
                        .build())
                .build();
    }

    @Override
    public Comparator<Long> getSorter(SearchOptions searchOptions) {
        return Comparator.<Long>naturalOrder().reversed();
    }

    @Override
    protected Long join(Long value, Long add) {
        return add;
    }

    @Override
    protected LeaderboardEntry<UUID, Long> fetchPlayerData(@NotNull UUID player, @NotNull SearchOptions options, @NotNull Database database) throws UnsupportedOperationException {
        return LeaderboardEntry.of(player, fetch(options, database, player));
    }


    @Override
    protected Long fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        final ProfessionFilter filter = (ProfessionFilter) options.getFilter();
        return professionRepository.getMostExperiencePerProfessionForGamer(entry, Objects.requireNonNull(filter).getProfession()).join();
    }

    @SneakyThrows
    @Override
    protected Map<UUID, Long> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        final ProfessionFilter filter = (ProfessionFilter) options.getFilter();
        return professionRepository.getMostExperiencePerProfession(Objects.requireNonNull(filter).getProfession()).join();
    }

    @Override
    protected CompletableFuture<Description> describe(SearchOptions searchOptions, LeaderboardEntry<UUID, Long> value) {

        final CompletableFuture<Description> future = new CompletableFuture<>();
        if (value.getValue() == null) {
            future.complete(null);
            return future;
        }


        ItemStack itemStack = new ItemStack(Material.PLAYER_HEAD);
        final OfflinePlayer player = Bukkit.getOfflinePlayer(value.getKey());
        if(player.getName() != null) {
            final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
            meta.setPlayerProfile(PlayerProfiles.CACHE.get(player.getUniqueId(), key -> player.isOnline() ? player.getPlayerProfile() : null));
            itemStack.setItemMeta(meta);
        }else {
            itemStack = new ItemStack(Material.PIGLIN_HEAD);
        }

        // Update name when loaded
        ItemStack finalItemStack = itemStack;
        this.clientManager.search().offline(player.getUniqueId(), clientOpt -> {
            final Map<String, Component> result = new LinkedHashMap<>();
            result.put("Player", Component.text(clientOpt.map(Client::getName).orElse(player.getUniqueId().toString())));
            Long experience = value.getValue();
            result.put("Level", Component.text(UtilFormat.formatNumber(getLevelFromExperience(experience))));
            result.put("Experience", Component.text(NumberFormat.getInstance().format(experience)));


            final Description description = Description.builder()
                    .icon(finalItemStack)
                    .properties(result)
                    .build();
            future.complete(description);
        }, true);

        return future;
    }

    private int getLevelFromExperience(long experience) {
        int level = 1;
        double expForNextLevel = 25;

        double experienceCopy = experience;

        while (experienceCopy >= expForNextLevel) {
            level++;
            experienceCopy -= expForNextLevel;
            expForNextLevel *= 1.01;
        }

        return level;
    }

    @Override
    public @NotNull FilterType[] acceptedFilters() {
        return this.filters;
    }
}
