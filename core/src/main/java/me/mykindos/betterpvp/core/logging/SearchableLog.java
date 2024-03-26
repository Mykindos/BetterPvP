package me.mykindos.betterpvp.core.logging;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a class that could contain 1-many multiple seriliazable statements for a single log
 */

@Getter
public abstract class SearchableLog {

    protected UUID LogUUID;
    protected final long time;
    protected final List<Statement> statements = new ArrayList<>();

    protected SearchableLog(UUID LogUUID) {
        this.LogUUID = LogUUID;
        this.time = System.currentTimeMillis();
        statements.add(getLogTimeStatetment());
    }

    private Statement getLogTimeStatetment() {
        return new Statement("INSERT INTO logtimes (id, Time) VALUES (?, ?)",
                new UuidStatementValue(this.LogUUID),
                new LongStatementValue(this.time));
    }
}
