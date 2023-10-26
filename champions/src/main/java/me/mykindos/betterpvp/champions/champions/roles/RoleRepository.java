package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class RoleRepository {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
    private String databasePrefix;

    private final Database database;

    @Getter
    private final Set<Role> roles = new HashSet<Role>();

    @Inject
    public RoleRepository(Database database) {
        this.database = database;
    }


    public void saveKillDeathData(Role killed, Role killer) {
        String killedRoleName = killed == null ? "NONE" : killed.getName();
        String killerRoleName = killer == null ? "NONE" : killer.getName();

        String killKey = killerRoleName + "_VS_" + killedRoleName;
        String deathKey = killedRoleName + "_VS_" + killerRoleName;

        String query = "INSERT INTO " + databasePrefix + "killdeath_data (Matchup, Metric, Value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Value = Value + 1";
        List<Statement> statements = new ArrayList<>();
        statements.add(new Statement(query, new StringStatementValue(killKey), new StringStatementValue("Kills"), new IntegerStatementValue(1)));
        statements.add(new Statement(query, new StringStatementValue(deathKey), new StringStatementValue("Deaths"), new IntegerStatementValue(1)));
        database.executeBatch(statements, true);
    }

}
