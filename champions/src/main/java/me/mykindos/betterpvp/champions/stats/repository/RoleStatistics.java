package me.mykindos.betterpvp.champions.stats.repository;

import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.stats.impl.ChampionsFilter;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.repository.StatHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

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
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix) {
        combatData.values().forEach(data -> data.prepareUpdates(uuid, database, databasePrefix));
    }
}
