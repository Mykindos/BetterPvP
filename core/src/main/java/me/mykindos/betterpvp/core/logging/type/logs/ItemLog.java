package me.mykindos.betterpvp.core.logging.type.logs;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.UUIDType;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class ItemLog extends SearchableLog {
    private final UUIDLogType type;
    private final List<Statement> locationStatements = new ArrayList<>();

    public ItemLog(UUID LogUUID, UUIDLogType type, UUID item) {
        super(LogUUID);
        this.type = type;
        addMeta(item, UUIDType.ITEM);
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
