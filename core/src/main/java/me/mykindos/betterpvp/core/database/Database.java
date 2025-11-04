package me.mykindos.betterpvp.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static me.mykindos.betterpvp.core.database.jooq.Tables.REALMS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.SERVERS;

/**
 * The Database class provides methods to execute database operations such as queries, updates,
 * batch operations, transactions, and stored procedures. It handles interactions with both local
 * and remote databases and provides asynchronous capabilities for certain operations.
 * <p>
 * This class uses dependency injection to initialize its components and includes timeout
 * handling for database operations to prevent hanging operations.
 */
@Getter
@CustomLog
@Singleton
public class Database {

    private final Core core;
    private final IDatabaseConnection connection;

    private static final Executor DB_EXECUTOR = Executors.newFixedThreadPool(10);

    @Getter
    private final DSLContext dslContext;
    /**
     * Constructs a new Database instance.
     *
     * @param core       The core component of the application, responsible for providing
     *                   access to the core functionalities and objects required by the Database.
     * @param connection The IDatabaseConnection instance, managing the connection to the
     *                   database and database-related operations.
     */
    @Inject
    public Database(Core core, IDatabaseConnection connection) {
        this.core = core;
        this.connection = connection;
        this.dslContext = DSL.using(
                connection.getDataSource(),
                SQLDialect.POSTGRES
        );
    }

    /**
     * Returns a DSLContext wrapper that executes queries asynchronously on a background thread.
     * Eliminates the need for CompletableFuture boilerplate in repository code.
     *
     * @return A functional interface that wraps queries with automatic async execution
     */
    public AsyncDSLContext getAsyncDslContext() {
        return new AsyncDSLContext(this.connection, DB_EXECUTOR);
    }


    public int getServerId(String serverName) {
        DSLContext dsl = getDslContext();

        return dsl.transactionResult(config -> {
            DSLContext ctx = DSL.using(config);

            // Get max ID + 1 for new server
            Integer maxId = ctx.select(DSL.coalesce(DSL.max(SERVERS.ID), 0).add(1))
                    .from(SERVERS)
                    .fetchOne(0, Integer.class);

            // Insert new server if it doesn't exist, return ID using RETURNING
            Integer serverId = ctx.insertInto(SERVERS)
                    .set(SERVERS.ID, maxId)
                    .set(SERVERS.NAME, serverName)
                    .onConflict(SERVERS.NAME)
                    .doNothing()
                    .returningResult(SERVERS.ID)
                    .fetchOne(SERVERS.ID);

            // If insert was skipped due to conflict, fetch existing ID
            if (serverId == null) {
                serverId = ctx.select(SERVERS.ID)
                        .from(SERVERS)
                        .where(SERVERS.NAME.eq(serverName))
                        .fetchOne(SERVERS.ID);
            }

            return serverId != null ? serverId : 0;
        });
    }

    public int getRealmId(int server, int season) {
        return dslContext.transactionResult(config -> {
            DSLContext ctx = DSL.using(config);

            // First, get the next ID
            Integer nextId = ctx.select(DSL.coalesce(DSL.max(REALMS.ID), 0).add(1))
                    .from(REALMS)
                    .fetchOne(0, Integer.class);

            // Then insert with the calculated ID
            Integer realmId = ctx.insertInto(REALMS)
                    .set(REALMS.ID, nextId)
                    .set(REALMS.SERVER, server)
                    .set(REALMS.SEASON, season)
                    .onConflict(REALMS.SERVER, REALMS.SEASON)
                    .doNothing()
                    .returningResult(REALMS.ID)
                    .fetchOne(REALMS.ID);

            // If insert was skipped due to conflict, fetch existing ID
            if (realmId == null) {
                realmId = ctx.select(REALMS.ID)
                        .from(REALMS)
                        .where(REALMS.SERVER.eq(server))
                        .and(REALMS.SEASON.eq(season))
                        .fetchOne(REALMS.ID);
            }

            return realmId != null ? realmId : 0;
        });
    }

}