package me.mykindos.betterpvp.clans.logging.types.log;

import me.mykindos.betterpvp.clans.logging.types.ClanLogType;
import me.mykindos.betterpvp.core.logging.type.logs.SearchableLog;

import java.util.UUID;

public class ClanLog extends SearchableLog {
    private final ClanLogType type;

    public ClanLog(UUID logUUID, ClanLogType type) {
        super(logUUID, type.name());
        this.type = type;
    }
}
