package me.mykindos.betterpvp.core.logging.type.logs;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDType;

import java.util.UUID;

public class MetaUuidLog {
    @Getter
    private UUID uuid;
    @Getter
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
