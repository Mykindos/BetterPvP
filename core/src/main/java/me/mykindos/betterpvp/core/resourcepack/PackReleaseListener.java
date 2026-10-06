package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reloads the release when the pack server sends NOTIFY resource_pack_published with this server's channel. Every
 * (re)connect reloads too, so a release published while the connection was down is never missed.
 */
@CustomLog
@Singleton
@BPvPListener
public class PackReleaseListener implements Listener {

    private final Database database;
    private final ResourcePackService service;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public PackReleaseListener(Database database, ResourcePackService service) {
        this.database = database;
        this.service = service;
    }

    @UpdateEvent(delay = 5000)
    public void startOnce() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        final Thread thread = new Thread(this::listenLoop, "bpvp-pack-release-listen");
        thread.setDaemon(true);
        thread.start();
    }

    private void listenLoop() {
        final DataSource dataSource = database.getConnection().getDataSource();
        while (!Thread.currentThread().isInterrupted()) {
            try (Connection connection = dataSource.getConnection()) {
                final PGConnection pgConnection = connection.unwrap(PGConnection.class);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN resource_pack_published");
                }
                service.reload();
                while (!connection.isClosed()) {
                    final PGNotification[] notifications = pgConnection.getNotifications(5000);
                    if (notifications == null) {
                        continue;
                    }
                    for (PGNotification notification : notifications) {
                        if (service.getChannel().equals(notification.getParameter())) {
                            service.reload();
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Resource pack release listener dropped, reconnecting in 5s", ex).submit();
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
        }
    }

}
