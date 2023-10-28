package me.mykindos.betterpvp.champions.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.impl.ChampionsCombatLeaderboard;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsCombatData;
import me.mykindos.betterpvp.champions.stats.repository.ChampionsStatsRepository;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.stats.model.CombatStatsListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

@BPvPListener
@Singleton
public class ChampionsStatsListener extends CombatStatsListener<ChampionsCombatData> {

    private final ChampionsStatsRepository repository;
    private final RoleManager roleManager;

    @Inject
    public ChampionsStatsListener(DamageLogManager logManager, ChampionsStatsRepository repository, ChampionsCombatLeaderboard leaderboard, RoleManager roleManager) {
        super(logManager, leaderboard);
        this.repository = repository;
        this.roleManager = roleManager;
    }

    @Override
    protected ChampionsStatsRepository getAssignedRepository(Player player) {
        return repository;
    }

    @Override
    protected CompletableFuture<ChampionsCombatData> getCombatData(Player player) {
        Role role = roleManager.getObject(player.getUniqueId()).orElse(null);
        return getAssignedRepository(player).getDataAsync(player).thenApply(data -> data.getCombatData(role));
    }
}
