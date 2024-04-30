package me.mykindos.betterpvp.core.logging.type.logs;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.UUIDType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a class that could contain 1-many multiple seriliazable statements for a single log
 */

@Getter
public abstract class SearchableLog {

    protected UUID logUUID;
    protected final long time;
    protected String logType;
    protected final List<Statement> statements = new ArrayList<>();
    protected final List<MetaUuidLog> metaUuidLogList = new ArrayList<>();

    protected SearchableLog(UUID logUUID, String type) {
        this.logUUID = logUUID;
        this.time = System.currentTimeMillis();
        this.logType = type;
    }

    public SearchableLog addMeta(UUID uuid, UUIDType uuidType) {
        MetaUuidLog metaUuidLog = new MetaUuidLog(uuid, uuidType);
        statements.add(metaUuidLog.getStatement(this.logUUID));
        metaUuidLogList.add(metaUuidLog);
        return this;
    }

    public Statement getLogTimeStatetment() {
        return new Statement("INSERT INTO logmeta (id, Time, Type) VALUES (?, ?, ?)",
                new UuidStatementValue(this.logUUID),
                new LongStatementValue(this.time),
                new StringStatementValue(logType));
    }
}
