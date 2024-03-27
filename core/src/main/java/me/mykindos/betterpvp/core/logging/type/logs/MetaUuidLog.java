package me.mykindos.betterpvp.core.logging.type.logs;

import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDType;

import java.util.UUID;

public class MetaUuidLog {
    private UUID uuid;
    private UUIDType uuidtype;

    public MetaUuidLog(java.util.UUID uuid, UUIDType uuidType) {
        this.uuid = uuid;
        this.uuidtype = uuidType;
    }

    public Statement getStatement(UUID LogUUID) {
        return new Statement("INSERT INTO logmetauuid (LogUUID, UUID, UUIDType) VALUES (?, ?, ?)",
                new UuidStatementValue(LogUUID),
                new UuidStatementValue(uuid),
                new StringStatementValue(uuidtype.name()));
    }
}
