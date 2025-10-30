package me.mykindos.betterpvp.core.client.offlinemessages;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.jooq.tables.records.GetOfflineMessagesByTimeRecord;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_OFFLINE_MESSAGES_BY_TIME;

@Singleton
@CustomLog
public class OfflineMessagesRepository implements IRepository<OfflineMessage> {
    private final Database database;

    @Inject
    public OfflineMessagesRepository(Database database) {
        this.database = database;
    }

    @Override
    public void save(OfflineMessage offlineMessage) {
        String query = "INSERT INTO offline_messages (Client, Time, Action, Message) VALUES (?, ?, ?, ?)";
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Core.class), () -> {
            Statement statement = new Statement(query,
                    new UuidStatementValue(offlineMessage.getClient()),
                    new LongStatementValue(offlineMessage.getTime()),
                    new StringStatementValue(offlineMessage.getAction().name()),
                    new StringStatementValue(offlineMessage.getRawContent())
            );

            database.executeUpdate(statement, TargetDatabase.GLOBAL);
            log.info("Saved offline message {} to database", offlineMessage).submit();
        });
    }

    /**
     * Gets the offline messages sent after last logout
     * Must be called after client properties are loaded
     *
     * @param client the client to load the messages for
     * @return the list of offline messages, sorted by most recent
     */
    public CompletableFuture<List<OfflineMessage>> getNewOfflineMessagesForClient(Client client) {
        return getOfflineMessagesForClient(client.getId(), (long) client.getProperty(ClientProperty.LAST_LOGIN).orElse(0));
    }

    /**
     * Gets the offline messages sent after time
     *
     * @param clientID the id of the client to load the messages for
     * @param time     the start time to retrieve messages
     * @return the list of offline messages, sorted by most recent
     */
    public CompletableFuture<List<OfflineMessage>> getOfflineMessagesForClient(long clientID, long time) {
        return CompletableFuture.supplyAsync(() -> {
            List<OfflineMessage> offlineMessages = new ArrayList<>();
            try {
                Result<GetOfflineMessagesByTimeRecord> offlineMessageRecords = GET_OFFLINE_MESSAGES_BY_TIME(database.getDslContext().configuration(), clientID, time);
                offlineMessageRecords.forEach(result -> {
                    UUID clientUUID = UUID.fromString(result.getClientUuid());
                    long messageTime = result.getTimeSent();
                    String messageAction = result.getAction();
                    String messageMessage = result.getMessage();

                    offlineMessages.add(new OfflineMessage(
                                    clientUUID,
                                    messageTime,
                                    OfflineMessage.Action.fromString(messageAction),
                                    messageMessage
                            )
                    );
                });
            } catch (DataAccessException e) {
                log.error("Error while retrieving offline messages for client {}", clientID, e).submit();
            }
            return offlineMessages;


        });
    }
}
