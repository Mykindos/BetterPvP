package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class RoleRepository {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
    private String databasePrefix;

    private final Database database;

    @Inject
    public RoleRepository(Database database) {
        this.database = database;
    }


    public void saveKillDeathData(Role killed, Role killer) {
        String killedRoleName = killed == null ? "NONE" : killed.name();
        String killerRoleName = killer == null ? "NONE" : killer.name();

        String killKey = killerRoleName + "_VS_" + killedRoleName;
        String deathKey = killedRoleName + "_VS_" + killerRoleName;

        String query = "INSERT INTO " + databasePrefix + "killdeath_data (Matchup, Metric, Value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE Value = Value + 1";
        List<Statement> statements = new ArrayList<>();
        statements.add(new Statement(query, new StringStatementValue(killKey), new StringStatementValue("Kills"), new IntegerStatementValue(1)));
        statements.add(new Statement(query, new StringStatementValue(deathKey), new StringStatementValue("Deaths"), new IntegerStatementValue(1)));
        database.executeBatch(statements, true);
    }
}
