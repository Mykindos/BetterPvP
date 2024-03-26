package me.mykindos.betterpvp.core.logging;

import me.mykindos.betterpvp.core.database.query.Statement;

import java.util.UUID;

public interface SearchableMeta {
    Statement getStatement(UUID LogUUID);
}
