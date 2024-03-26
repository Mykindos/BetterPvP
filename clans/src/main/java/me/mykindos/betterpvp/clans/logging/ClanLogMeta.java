package me.mykindos.betterpvp.clans.logging;

import lombok.Data;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.logging.SearchableMeta;

import java.util.UUID;

@Data
public class ClanLogMeta implements SearchableMeta {


    private UUID uuid;
    private UUIDType uuidtype;

    public ClanLogMeta(java.util.UUID uuid, UUIDType uuidType) {
        this.uuid = uuid;
        this.uuidtype = uuidType;
    }

    @Override
    public Statement getStatement(UUID LogUUID) {
        return new Statement("INSERT INTO clanlogmeta (LogUUID, UUID, UUIDType) VALUES (?, ?, ?)",
                new UuidStatementValue(LogUUID),
                new UuidStatementValue(uuid),
                new StringStatementValue(uuidtype.name()));
    }

    public enum UUIDType {
        /**
         * Represents the primary player, generally the player doing the action
         */
        PLAYER1,
        /**
         * Represents a secondary player, general the player the action occurs to
         */
        PLAYER2,
        /**
         * The clan of PLAYER1
         */
        CLAN1,
        /**
         * The clan of PLAYER2
         */
        CLAN2
    }
}
