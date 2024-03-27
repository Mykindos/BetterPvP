package me.mykindos.betterpvp.clans.logging.types.log;

import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.type.logs.SearchableLog;

import java.util.UUID;

public class ClanLog extends SearchableLog {
    private final ClanLogType type;

    public ClanLog(UUID LogUUID, ClanLogType type) {
        super(LogUUID);
        this.type = type;
    }

    public Statement getClanLogStatement() {
        return new Statement("INSERT INTO clanlogtype (LogUUID, type) VALUES (?, ?)",
                new UuidStatementValue(this.LogUUID),
                new StringStatementValue(this.type.name()));
    }

}
