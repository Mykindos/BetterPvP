package me.mykindos.betterpvp.clans.logging;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@CustomLog
public class ClanLogger {

    private static Database database;

    @Inject
    public ClanLogger(Database database) {
        ClanLogger.database = database;
    }

    /**
     * @param formattedMessage the formatted message. Will log a message without
     * @param args
     */
    public static UUID addClanLog(String formattedMessage, Object... args) {
        //TODO de-format formattedMessage
        String message = formattedMessage;
        return addClanLog(message, formattedMessage, args);
    }

    public static UUID addClanLog(String message, String formattedMessage, Object... args) {
        UUID id = log.info(message);
        //TODO log formattedMessage to proper place
        return id;
    }

    /**
     * Add a reference to the specified log id for a clan log
     * @param logUUID the ID of the log this information is about
     * @param uuid the UUID to reference
     * @param uuidType the type of UUID this is
     * @param type the type of log this is
     */
    public static void addClanLogMeta(UUID logUUID, @Nullable UUID uuid, ClanLogger.UUIDType uuidType, ClanLogger.ClanLogType type) {
        String query = "INSERT INTO clanlogmeta (logID, UUID, UUIDType, type) VALUES (?, ?, ?, ?)";
        database.executeUpdate(new Statement(query,
                        new UuidStatementValue(logUUID),
                        new StringStatementValue(uuid == null ? null : uuid.toString()),
                        new StringStatementValue(uuidType.name()),
                        new StringStatementValue(type.name())
                )
        );
    }

    public enum UUIDType {
        PLAYER,
        CLAN,
        NONE
    }

    public enum ClanLogType {
        /**
         * The player joins a Clan
         */
        JOIN,
        /**
         * The player leaves a Clan
         */
        LEAVE,
        /**
         * The player kicks another player from a Clan
         */
        KICKER,
        /**
         * The player is kicked from a Clan
         */
        KICKED
        //TODO more enums
    }
}
