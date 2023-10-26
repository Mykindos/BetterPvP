package me.mykindos.betterpvp.champions.stats.repository;

import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.stats.repository.StatHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

public class RoleStatistics extends StatHolder {

    private final Map<Role, ChampionsCombatData> combatData;

    public RoleStatistics(Map<Role, ChampionsCombatData> combatData) {
        this.combatData = combatData;
    }

    public ChampionsCombatData getCombatData(@Nullable Role role) {
        return combatData.get(role);
    }

    @Override
    protected void prepareUpdates(@NotNull UUID uuid, @NotNull Database database, String databasePrefix) {
        combatData.values().forEach(data -> data.prepareUpdates(uuid, database, databasePrefix));
    }
}
