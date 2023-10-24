package me.mykindos.betterpvp.champions.champions.roles;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Singleton
public class RoleRepository {

    @Inject
    @Config(path = "champions.database.prefix", defaultValue = "champions_")
    private String databasePrefix;

    private final Database database;

    @Inject
    private final Champions champions;

    private final Set<Role> roles;

    public RoleRepository(Database database, Champions champions) {
        this.database = database;
        this.champions = champions;
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

    private void loadRoles() {
        ExtendedYamlConfiguration config = champions.getConfig();
        String path = "class";
        ConfigurationSection customRoleSection = config.getConfigurationSection(path);
        if (customRoleSection == null) {
            customRoleSection = config.createSection(path);
        }
        for (String key : customRoleSection.getKeys(false)) {
            final ConfigurationSection section = customRoleSection.getConfigurationSection(key);
            final Role loaded = new Role(section.getName());
            loaded.loadConfig(config);
        }
    }
}
