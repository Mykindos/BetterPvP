package me.mykindos.betterpvp.champions.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatLeaderboard;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatStatsRepository;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.SearchOptions;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

@Singleton
@Slf4j
public class ChampionsCombatLeaderboard extends GlobalCombatLeaderboard {

    @Inject
    public ChampionsCombatLeaderboard(Core core, GlobalCombatStatsRepository repository) {
        super(core, repository);
    }

    @Override
    protected CombatData fetch(@NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix, @NotNull UUID entry) {
        return super.fetch(options, database, tablePrefix, entry);
    }

    @Override
    protected Map<UUID, CombatData> fetchAll(@NotNull SearchOptions options, @NotNull Database database, @NotNull String tablePrefix) {
        return super.fetchAll(options, database, tablePrefix);
    }
}
