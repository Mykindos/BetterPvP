package me.mykindos.betterpvp.proxy.resourcepack;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.mykindos.betterpvp.proxy.ProxyConfig;
import org.postgresql.Driver;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Keeps the newest release of the configured channel. It listens for NOTIFY resource_pack_published and reloads on
 * every (re)connect, so a release published while the connection was down is never missed.
 */
public final class PackReleaseFeed {

    private static final String CHANNEL = "resource_pack_published";

    private final ProxyConfig config;
    private final Logger logger;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<PackRelease> current = new AtomicReference<>();
    private final BiConsumer<PackRelease, PackRelease> onChange;

    /**
     * @param onChange called with the previous release (null on the first load) and the new one
     */
    public PackReleaseFeed(ProxyConfig config, Logger logger, BiConsumer<PackRelease, PackRelease> onChange) {
        this.config = config;
        this.logger = logger;
        this.onChange = onChange;
    }

    public Optional<PackRelease> current() {
        return Optional.ofNullable(current.get());
    }

    public void start() {
        final Thread thread = new Thread(this::listenLoop, "bpvp-pack-release-listen");
        thread.setDaemon(true);
        thread.start();
    }

    private void listenLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try (Connection connection = connect()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN " + CHANNEL);
                }
                reload(connection);
                final PGConnection pgConnection = connection.unwrap(PGConnection.class);
                while (!connection.isClosed()) {
                    final PGNotification[] notifications = pgConnection.getNotifications(5000);
                    if (notifications == null) {
                        continue;
                    }
                    for (PGNotification notification : notifications) {
                        if (config.packChannel().equals(notification.getParameter())) {
                            reload(connection);
                            break;
                        }
                    }
                }
            } catch (Exception ex) {
                logger.warn("Resource pack release listener dropped, reconnecting in 5s", ex);
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException interrupted) {
                    return;
                }
            }
        }
    }

    /**
     * Opens the connection through the driver itself, since DriverManager cannot see a driver in a plugin's
     * class loader.
     */
    private Connection connect() throws SQLException {
        final Properties properties = new Properties();
        properties.setProperty("user", config.packDatabaseUser());
        properties.setProperty("password", config.packDatabasePassword());
        final Connection connection = new Driver().connect(config.packDatabaseUrl(), properties);
        if (connection == null) {
            throw new SQLException("Not a PostgreSQL JDBC url: " + config.packDatabaseUrl());
        }
        return connection;
    }

    private void reload(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select manifest::text from resource_pack_release where channel = ? order by id desc limit 1")) {
            statement.setString(1, config.packChannel());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    logger.warn("No resource pack release published to channel {}", config.packChannel());
                    return;
                }
                final PackRelease release = mapper.readValue(result.getString(1), PackRelease.class);
                final PackRelease previous = current.getAndSet(release);
                if (previous == null || !previous.getVersion().equals(release.getVersion())) {
                    logger.info("Resource pack release {} loaded from channel {}", release.getVersion(), config.packChannel());
                    onChange.accept(previous, release);
                }
            }
        }
    }

}
