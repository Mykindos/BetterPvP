package me.mykindos.betterpvp.progression.profession.mining.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.database.jooq.tables.records.GetTopMiningByOreRecord;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.mykindos.betterpvp.progression.database.jooq.Tables.GET_TOP_MINING_BY_ORE;

@CustomLog
@Singleton
public class MiningOresMinedLeaderboard extends PlayerLeaderboard<Integer> {

    private final MiningHandler miningHandler;

    @Inject
    public MiningOresMinedLeaderboard(Progression progression, MiningHandler miningHandler) {
        super(progression);
        this.miningHandler = miningHandler;
        init();
    }

    @Override
    public String getName() {
        return "Total Ores Mined";
    }

    @Override
    public LeaderboardCategory getCategory() {
        return LeaderboardCategory.PROFESSION;
    }

    @Override
    public Description getDescription() {
        return Description.builder()
                .icon(ItemView.builder()
                        .material(Material.IRON_PICKAXE)
                        .flag(ItemFlag.HIDE_ATTRIBUTES)
                        .displayName(Component.text("Total Ores Mined", NamedTextColor.AQUA))
                        .build())
                .build();
    }

    @Override
    public Comparator<Integer> getSorter(SearchOptions searchOptions) {
        return Comparator.comparing(Integer::intValue).reversed();
    }

    @Override
    protected Integer join(Integer value, Integer add) {
        return value + add;
    }

    @Override
    protected Integer fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        return miningHandler.getMiningRepository().getOresMinedForGamer(entry).join();
    }

    @Override
    @SneakyThrows
    protected Map<UUID, Integer> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        Map<UUID, Integer> leaderboard = new HashMap<>();
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            try {
                Result<GetTopMiningByOreRecord> results = ctx.selectFrom(GET_TOP_MINING_BY_ORE.call(
                        Core.getCurrentRealm().getSeason(),
                        10,
                        miningHandler.getDbMaterialsArray())).fetch();

                results.forEach(oreRecord -> {
                    leaderboard.put(UUID.fromString(oreRecord.getClientUuid()), oreRecord.getTotalAmountMined());
                });
            } catch (DataAccessException ex) {
                log.error("Error fetching leaderboard data", ex).submit();
            }
        });

        return leaderboard;
    }

}
