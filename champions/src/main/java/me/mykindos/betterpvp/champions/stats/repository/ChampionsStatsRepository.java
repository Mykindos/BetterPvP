package me.mykindos.betterpvp.champions.stats.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.impl.ChampionsFilter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.combat.stats.impl.GlobalCombatStatsRepository;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.champions.database.jooq.tables.records.GetChampionsDataRecord;
import me.mykindos.betterpvp.core.stats.repository.StatsRepository;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.champions.database.jooq.Tables.CHAMPIONS_COMBAT_STATS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.COMBAT_STATS;
import static me.mykindos.betterpvp.champions.database.jooq.Tables.GET_CHAMPIONS_DATA;
import static me.mykindos.betterpvp.core.database.jooq.Tables.KILLS;

@Singleton
@CustomLog
public class ChampionsStatsRepository extends StatsRepository<RoleStatistics> {

    private final RoleManager roleManager;

    @Inject
    public ChampionsStatsRepository(Champions champions, RoleManager roleManager, GlobalCombatStatsRepository globalRepo) {
        super(champions);
        this.roleManager = roleManager;
        globalRepo.addDependentRepository(this);
    }

    @Override
    public CompletableFuture<RoleStatistics> fetchDataAsync(UUID player) {
        final Map<ChampionsFilter, ChampionsCombatData> combatDataMap = new EnumMap<>(ChampionsFilter.class);
        final RoleStatistics roleStatistics = new RoleStatistics(combatDataMap, roleManager, player);
        return database.getAsyncDslContext().executeAsync(ctx -> {

            Result<GetChampionsDataRecord> dataRecords = GET_CHAMPIONS_DATA(ctx.configuration(), player.toString(), Core.getCurrentRealm());
            dataRecords.forEach(result -> {
                String className = result.getClass_();
                Role role = className.isEmpty() ? null : Role.valueOf(className);
                ChampionsFilter filter = ChampionsFilter.fromRole(role);
                ChampionsCombatData data = new ChampionsCombatData(player, roleManager, role);
                data.setKills(result.getKills());
                data.setDeaths(result.getDeaths());
                data.setAssists(result.getAssists());
                data.setKillStreak(data.getKillStreak());
                data.setHighestKillStreak(data.getHighestKillStreak());

                int rating = result.getRating();
                data.setRating(rating);

                combatDataMap.put(filter, data);

            });


            return roleStatistics;
        }).exceptionally(throwable -> {
            log.error("Failed to load combat data for " + player, throwable).submit();
            return null;
        });
    }

    public void validate(Client client, boolean isValid) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.transaction(configuration -> {
                DSLContext ctxl = DSL.using(configuration);

                ctxl.update(KILLS).set(KILLS.VALID, isValid).where(KILLS.KILLER.eq(client.getId())).execute();
                ctxl.update(COMBAT_STATS).set(COMBAT_STATS.VALID, isValid).where(COMBAT_STATS.CLIENT.eq(client.getId())).execute();
                ctxl.update(CHAMPIONS_COMBAT_STATS).set(CHAMPIONS_COMBAT_STATS.VALID, isValid).where(CHAMPIONS_COMBAT_STATS.CLIENT.eq(client.getId())).execute();
            });
        });
    }
}
