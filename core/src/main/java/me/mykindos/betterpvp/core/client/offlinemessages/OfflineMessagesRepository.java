package me.mykindos.betterpvp.core.client.offlinemessages;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.properties.ClientProperty;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
     * @param client the client to load the messages for
     * @return the list of offline messages, sorted by most recent
     */
    public CompletableFuture<List<OfflineMessage>> getNewOfflineMessagesForClient(Client client) {
        return getOfflineMessagesForClient(client.getUniqueId(), (long) client.getProperty(ClientProperty.LAST_LOGIN).orElse(0));
    }

    /**
     * Gets the offline messages sent after time
     * @param clientID the id of the client to load the messages for
     * @param time the start time to retrieve messages
     * @return the list of offline messages, sorted by most recent
     */
    public CompletableFuture<List<OfflineMessage>> getOfflineMessagesForClient(UUID clientID, long time) {
        CompletableFuture<List<OfflineMessage>> listFuture = new CompletableFuture<>();
        listFuture.completeAsync(() -> {
            List<OfflineMessage> offlineMessages = new ArrayList<>();
            String query = "CALL GetOfflineMessagesByTime(?, ?);";
            Statement statement = new Statement(query,
                    new UuidStatementValue(clientID),
                    new LongStatementValue(time)
            );

            CachedRowSet result = database.executeQuery(statement, TargetDatabase.GLOBAL);
            try {
                while (result.next()) {


                    long messageTime = result.getLong(1);
                    String messageAction = result.getString(2);
                    String messageMessage = result.getString(3);

                    offlineMessages.add(new OfflineMessage(
                                    clientID,
                                    messageTime,
                                    OfflineMessage.Action.fromString(messageAction),
                                    messageMessage
                            )
                    );
                }
            } catch (SQLException e) {
                log.error("Error while retrieving offline messages for client {}", clientID, e).submit();
            }
            return offlineMessages;
        });
        return listFuture;
    }
}
