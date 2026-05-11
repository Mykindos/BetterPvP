package me.mykindos.betterpvp.champions.stats.repository;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.impl.ChampionsFilter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.repository.StatHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RoleStatistics extends StatHolder {

    private final Map<ChampionsFilter, ChampionsCombatData> combatData;
    private final RoleManager roleManager;
    private final UUID player;

    public RoleStatistics(Map<ChampionsFilter, ChampionsCombatData> combatData, RoleManager roleManager, UUID player) {
        this.combatData = combatData;
        this.roleManager = roleManager;
        this.player = player;
    }

    public ChampionsCombatData getCombatData(@Nullable Role role) {
        return getCombatData(ChampionsFilter.fromRole(role));
    }

    public ChampionsCombatData getCombatData(@NotNull ChampionsFilter filter) {
        combatData.computeIfAbsent(filter, f -> new ChampionsCombatData(player, roleManager, filter.getRole()));
        return combatData.get(filter);
    }

    @Override
    protected CompletableFuture<Void> prepareUpdates(@NotNull UUID uuid, @NotNull Database database) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        combatData.values().forEach(data -> futures.add(data.prepareUpdates(uuid, database)));
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
