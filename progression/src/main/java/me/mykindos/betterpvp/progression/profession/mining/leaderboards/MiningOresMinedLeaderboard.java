package me.mykindos.betterpvp.progression.profession.mining.leaderboards;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.PlayerLeaderboard;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import me.mykindos.betterpvp.core.utilities.model.description.Description;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CustomLog
@Singleton
public class MiningOresMinedLeaderboard extends PlayerLeaderboard<Long> {

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
    public Comparator<Long> getSorter(SearchOptions searchOptions) {
        return Comparator.comparing(Long::intValue).reversed();
    }

    @Override
    protected Long join(Long value, Long add) {
        return value + add;
    }

    @Override
    protected Long fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull UUID entry) {
        return miningHandler.getMiningRepository().getOresMinedForGamer(entry).join();
    }

    @Override
    @SneakyThrows
    protected Map<UUID, Long> fetchAll(@NotNull SearchOptions options, @NotNull Database database) {
        Map<UUID, Long> leaderboard = new HashMap<>();
        Statement statement = new Statement("CALL GetTopMiningByOre(?, ?)",
                new IntegerStatementValue(10),
                new StringStatementValue(miningHandler.getDbMaterialsList()));
        database.executeProcedure(statement, -1, result -> {
            try {
                while (result.next()) {
                    final String gamer = result.getString(1);
                    final long count = result.getLong(2);
                    leaderboard.put(UUID.fromString(gamer), count);
                }
            } catch (SQLException e) {
                log.error("Error fetching leaderboard data", e).submit();
            }
        });

        return leaderboard;
    }

}
