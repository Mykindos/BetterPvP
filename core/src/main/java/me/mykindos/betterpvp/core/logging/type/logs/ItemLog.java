package me.mykindos.betterpvp.core.logging.type.logs;

import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemLog extends SearchableLog {
    private final UUIDLogType type;
    private final List<Statement> locationStatements = new ArrayList<>();

    public ItemLog(UUID LogUUID, UUIDLogType type) {
        super(LogUUID);
        this.type = type;
    }

    public ItemLog addLocation(Location location, String name) {
        LocationLog locationLog = new LocationLog(location, name);
        statements.add(locationLog.getStatement(this.LogUUID));
        return this;
    }

    public Statement getItemLogStatement() {
        return new Statement("INSERT INTO clanlogtype (LogUUID, type) VALUES (?, ?)",
                new UuidStatementValue(this.LogUUID),
                new StringStatementValue(this.type.name()));
    }
}
