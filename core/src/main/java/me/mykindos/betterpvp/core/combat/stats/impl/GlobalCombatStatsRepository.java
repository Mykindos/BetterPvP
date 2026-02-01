package me.mykindos.betterpvp.core.combat.stats.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.stats.model.CombatStatsRepository;
import me.mykindos.betterpvp.core.combat.stats.model.ICombatDataAttachment;
import me.mykindos.betterpvp.core.database.jooq.tables.records.GetCombatDataRecord;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_COMBAT_DATA;

@CustomLog
@Singleton
public class GlobalCombatStatsRepository extends CombatStatsRepository<GlobalCombatData> {

    private final List<StatsRepository<?>> dependentRepositories = new ArrayList<>();

    @Inject
    protected GlobalCombatStatsRepository(Core plugin) {
        super(plugin);
    }

    public void addDependentRepository(StatsRepository<?> repository) {
        this.dependentRepositories.add(repository);
    }

    @Override
    protected void postSaveAll(boolean async) {
        dependentRepositories.forEach(repo -> repo.saveAll(async));
    }

    @Override
    public CompletableFuture<GlobalCombatData> fetchDataAsync(UUID player) {
        return CompletableFuture.supplyAsync(() -> {



            final GlobalCombatData data = new GlobalCombatData(player);

            try {
                GetCombatDataRecord combatRecord = GET_COMBAT_DATA(database.getDslContext().configuration(),
                        player.toString(),
                        Core.getCurrentRealm().getId())
                        .getFirst();

                if (combatRecord != null) {
                    data.setKills(combatRecord.getKills());
                    data.setDeaths(combatRecord.getDeaths());
                    data.setAssists(combatRecord.getAssists());
                    data.setKillStreak(combatRecord.getKillstreak());
                    data.setHighestKillStreak(combatRecord.getHighestKillstreak());

                    Integer rating = combatRecord.getRating();
                    if (rating != null) {
                        data.setRating(rating);
                    }
                }

                attachmentLoaders.forEach(loader -> {
                    final ICombatDataAttachment attachment = loader.loadAttachment(player, data, database);
                    data.attach(attachment);
                });
            } catch (Exception ex) {
                log.error("Failed to load combat data for " + player, ex);
            }
            return data;
        }).exceptionally(throwable -> {
            log.error("Failed to load combat data for " + player, throwable);
            return null;
        });
    }

}
