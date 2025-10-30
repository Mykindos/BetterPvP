package me.mykindos.betterpvp.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.StatementValue;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
    private static final TargetDatabase DEFAULT_DATABASE = TargetDatabase.GLOBAL;

    @Getter
    private final DSLContext dslContext;

    // Default timeout values
    private static final long DEFAULT_QUERY_TIMEOUT_SECONDS = 30;
    private static final long DEFAULT_UPDATE_TIMEOUT_SECONDS = 60;
    private static final long DEFAULT_BATCH_TIMEOUT_SECONDS = 600;
    private static final long DEFAULT_PROCEDURE_TIMEOUT_SECONDS = 60;
    private static final Executor WRITE_EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Executor READ_EXECUTOR = Executors.newFixedThreadPool(5);

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
                connection.getDatabaseConnection(DEFAULT_DATABASE),
                SQLDialect.POSTGRES
        );
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
        DSLContext dsl = getDslContext();

        return dsl.transactionResult(config -> {
            DSLContext ctx = DSL.using(config);


            Integer realmId = ctx.insertInto(REALMS)
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

    /**
     * Executes an update statement asynchronously on the local database.
     *
     * @param statement The database statement to be executed, containing the query and its parameters.
     * @return A CompletableFuture that completes when the update operation finishes
     */
    public CompletableFuture<Void> executeUpdateAsync(Statement statement) {
        return executeUpdateAsync(statement, DEFAULT_DATABASE);
    }

    /**
     * Executes an update asynchronously on the specified target database using the provided SQL statement.
     * The operation is executed in a separate thread to ensure non-blocking behavior.
     *
     * @param statement      The {@link Statement} object containing the SQL query and associated parameters.
     * @param targetDatabase The {@link TargetDatabase} enumeration indicating the database (LOCAL or GLOBAL)
     *                       where the query should be executed.
     * @return A CompletableFuture that completes when the update operation finishes
     */
    public CompletableFuture<Void> executeUpdateAsync(Statement statement, TargetDatabase targetDatabase) {
        return executeUpdate(statement, targetDatabase);
    }


    /**
     * Executes an update statement on the specified target database without any timeout restrictions.
     * This method should be used carefully as it may block indefinitely if the database operation hangs.
     *
     * @param statement      The SQL statement to be executed, including its query and associated parameters.
     * @param targetDatabase The target database where the update statement should be executed (e.g., LOCAL, GLOBAL).
     * @return A CompletableFuture that completes when the update operation finishes
     */
    public CompletableFuture<Void> executeUpdateNoTimeout(Statement statement, TargetDatabase targetDatabase) {
        return CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase);
                 PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {

                // No query timeout set - this allows the operation to run indefinitely
                setStatementParameters(preparedStatement, statement);
                preparedStatement.executeUpdate();

            } catch (SQLException ex) {
                log.error("Error executing update without timeout: {}", statement.getQuery(), ex).submit();
                throw new RuntimeException("Database update failed", ex);
            }
        }, WRITE_EXECUTOR).exceptionally(ex -> {
            log.error("Unexpected error in executeUpdateNoTimeout for query: {}", statement.getQuery(), ex).submit();
            return null;
        });
    }

    /**
     * Executes an update statement on the default (LOCAL) target database without any timeout restrictions.
     *
     * @param statement The SQL statement to be executed, including its query and associated parameters.
     * @return A CompletableFuture that completes when the update operation finishes
     */
    public CompletableFuture<Void> executeUpdateNoTimeout(Statement statement) {
        return executeUpdateNoTimeout(statement, DEFAULT_DATABASE);
    }

    /**
     * Executes an update SQL statement using the provided {@code Statement} object.
     * The execution is performed on the LOCAL target database.
     *
     * @param statement The {@code Statement} object containing the SQL query and parameters to be executed.
     * @return A CompletableFuture that completes when the update operation finishes
     */
    public CompletableFuture<Void> executeUpdate(Statement statement) {
        return executeUpdate(statement, DEFAULT_DATABASE);
    }

    /**
     * Executes an update statement on the specified target database. This method prepares
     * the SQL statement, sets the provided parameters, and executes it as an update operation.
     *
     * @param statement      The SQL statement to be executed, including its query and associated parameters.
     * @param targetDatabase The target database where the update statement should be executed (e.g., LOCAL, GLOBAL).
     * @return A CompletableFuture that completes when the update operation finishes
     */
    public CompletableFuture<Void> executeUpdate(Statement statement, TargetDatabase targetDatabase) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase);
                 PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {

                // Set query timeout in seconds
                preparedStatement.setQueryTimeout((int) DEFAULT_UPDATE_TIMEOUT_SECONDS);

                setStatementParameters(preparedStatement, statement);
                preparedStatement.executeUpdate();

            } catch (SQLException ex) {
                log.error("Error executing update: {}", statement.getQuery(), ex).submit();
            }
        }, WRITE_EXECUTOR).exceptionally(ex -> {
            log.error("Unexpected error in executeUpdate for query: {}", statement.getQuery(), ex).submit();
            return null;
        });

        // Add timeout at the CompletableFuture level as well
        return future.orTimeout(DEFAULT_UPDATE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.error("Timeout executing update: {}", statement.getQuery(), ex).submit();
                    } else {
                        log.error("Error in executeUpdate: {}", statement.getQuery(), ex).submit();
                    }
                    return null;
                });
    }

    /**
     * Executes a batch of {@link Statement} objects asynchronously against the default target database.
     * Each statement in the batch is processed in sequence as part of the operation.
     *
     * @param statements A list of {@link Statement} objects representing the SQL queries and their
     *                   associated parameters to be executed as a batch.
     * @return A {@link CompletableFuture} that represents the asynchronous execution of the batch.
     * The future completes when the batch execution finishes or exceptionally if an error occurs.
     */
    public CompletableFuture<Void> executeBatch(List<Statement> statements) {
        return executeBatch(statements, DEFAULT_DATABASE);
    }

    /**
     * Executes a batch of database statements in a transactional context asynchronously.
     * If the provided list of statements is empty, the method returns a completed CompletableFuture without executing anything.
     * Otherwise, it performs the statements within a transaction on the specified target database.
     *
     * @param statements     The list of {@link Statement} objects to be executed in the batch.
     *                       Each statement contains the SQL query and its associated parameters.
     * @param targetDatabase The {@link TargetDatabase} indicating the target database
     *                       where the batch of statements will be executed, such as LOCAL or GLOBAL.
     * @return A {@link CompletableFuture} that completes when the transaction finishes successfully,
     * or completes exceptionally if an error occurs during execution.
     */
    public CompletableFuture<Void> executeBatch(List<Statement> statements, TargetDatabase targetDatabase) {
        if (statements == null || statements.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
                executeTransactionalBatch(connection, statements);
            } catch (SQLException e) {
                log.error("Failed to manage transaction or close connection", e).submit();
                throw new RuntimeException(e); // This ensures the CompletableFuture completes exceptionally
            }
        }, WRITE_EXECUTOR).exceptionally(ex -> {
            log.error("Unexpected error in executeBatch", ex).submit();
            return null;
        });

        // Add timeout handling
        return future.orTimeout(DEFAULT_BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.error("Timeout executing batch with {} statements", statements.size(), ex).submit();
                    } else {
                        log.error("Error in executeBatch", ex).submit();
                    }
                    return null;
                });
    }

    /**
     * Executes a list of database statements as a transactional operation using a custom executor.
     * The transaction is performed asynchronously on the provided executor thread.
     *
     * @param statements     The list of {@link Statement} objects to execute within the transaction.
     *                       Each statement contains the SQL query and associated parameter values.
     * @param targetDatabase The target database where the transaction should be executed.
     *                       It can specify different database contexts such as LOCAL or GLOBAL.
     * @param executor       The {@link Executor} to use for running the transaction asynchronously.
     *                       This allows for custom thread pool management and execution control.
     * @return A CompletableFuture that completes when the transaction operation finishes
     */
    public CompletableFuture<Void> executeTransaction(List<Statement> statements, TargetDatabase targetDatabase, Executor executor, boolean catchErrors) {
        if (statements == null || statements.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
                executeTransactionalStatements(connection, statements, catchErrors);
            } catch (SQLException e) {
                if (catchErrors) {
                    log.error("Failed to manage transaction or close connection", e).submit();
                    throw new RuntimeException(e); // Ensure exceptional completion
                }
            }
        }, executor).exceptionally(ex -> {
            log.error("Unexpected error in executeTransaction", ex).submit();
            return null;
        });

        // Add timeout for the entire transaction
        return future.orTimeout(DEFAULT_BATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.error("Timeout executing transaction with {} statements", statements.size(), ex).submit();
                    } else {
                        log.error("Error in executeTransaction", ex).submit();
                    }
                    return null;
                });
    }

    /**
     * Executes a list of database statements as a transactional operation using a custom executor.
     * The transaction is performed on the default (LOCAL) target database.
     *
     * @param statements The list of {@link Statement} objects to execute within the transaction.
     *                   Each statement contains the SQL query and associated parameter values.
     * @param executor   The {@link Executor} to use for running the transaction asynchronously.
     * @return A CompletableFuture that completes when the transaction operation finishes
     */
    public CompletableFuture<Void> executeTransaction(List<Statement> statements, Executor executor) {
        return executeTransaction(statements, DEFAULT_DATABASE, executor, true);
    }

    /**
     * Executes a list of database statements as a transactional operation.
     * The transaction is always performed asynchronously on a dedicated thread.
     *
     * @param statements     The list of {@link Statement} objects to execute within the transaction.
     *                       Each statement contains the SQL query and associated parameter values.
     * @param targetDatabase The target database where the transaction should be executed.
     *                       It can specify different database contexts such as LOCAL or GLOBAL.
     * @return A CompletableFuture that completes when the transaction operation finishes
     */
    public CompletableFuture<Void> executeTransaction(List<Statement> statements, TargetDatabase targetDatabase) {
        return executeTransaction(statements, targetDatabase, WRITE_EXECUTOR, true);
    }

    public CompletableFuture<Void> executeTransaction(List<Statement> statements, TargetDatabase targetDatabase, boolean catchErrors) {
        return executeTransaction(statements, targetDatabase, WRITE_EXECUTOR, catchErrors);
    }

    /**
     * Executes a series of database statements within a single transaction.
     * By default, the target database is set to {@code TargetDatabase.GLOBAL}.
     *
     * @param statements A list of {@link Statement} objects representing the SQL queries
     *                   and their associated parameters to be executed as part of the transaction.
     * @return A CompletableFuture that completes when the transaction operation finishes
     */
    public CompletableFuture<Void> executeTransaction(List<Statement> statements) {
        return executeTransaction(statements, DEFAULT_DATABASE, WRITE_EXECUTOR, true);
    }


    /**
     * Executes the given SQL statement and retrieves the result as a cached row set.
     * The query is executed against the local database by default.
     *
     * @param statement The {@link Statement} object containing the SQL query
     *                  and related parameters such as values and filters.
     * @return A CompletableFuture containing a {@link CachedRowSet} object populated with the results of the query execution.
     * Returns null if the query execution fails or encounters an error.
     */
    public CompletableFuture<CachedRowSet> executeQuery(Statement statement) {
        return executeQuery(statement, DEFAULT_DATABASE);
    }

    /**
     * Executes the provided SQL query represented by the given {@link Statement} against the specified
     * {@link TargetDatabase}. Retrieves the query results and populates a {@link CachedRowSet}.
     *
     * @param statement      The {@link Statement} instance containing the SQL query string and associated values.
     * @param targetDatabase The target database on which the query should be executed, specified using {@link TargetDatabase}.
     * @return A CompletableFuture containing a {@link CachedRowSet} containing the results of the query.
     * If an error occurs or the query fails, it may return {@code null}.
     */
    public CompletableFuture<CachedRowSet> executeQuery(Statement statement, TargetDatabase targetDatabase) {
        CompletableFuture<CachedRowSet> future = CompletableFuture.supplyAsync(() -> {
            CachedRowSet rowset = null;

            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase);
                 PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {

                RowSetFactory factory = RowSetProvider.newFactory();
                rowset = factory.createCachedRowSet();

                // Set query timeout in seconds
                preparedStatement.setQueryTimeout((int) DEFAULT_QUERY_TIMEOUT_SECONDS);

                setStatementParameters(preparedStatement, statement);
                rowset.populate(preparedStatement.executeQuery());

            } catch (SQLException ex) {
                log.error("Error executing query: {}", statement.getQuery(), ex).submit();

                try {
                    if (rowset != null) {
                        rowset.close();
                    }
                } catch (SQLException closeEx) {
                    log.error("Error closing rowset", closeEx).submit();
                }

            }

            return rowset;
        }, READ_EXECUTOR).exceptionally(ex -> {
            log.error("Unexpected error in executeQuery for query: {}", statement.getQuery(), ex).submit();
            return null;
        });

        // Add CompletableFuture timeout
        return future.orTimeout(DEFAULT_QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.error("Timeout executing query: {}", statement.getQuery(), ex).submit();
                    } else {
                        log.error("Error in executeQuery: {}", statement.getQuery(), ex).submit();
                    }
                    return null;
                });
    }

    /**
     * Executes a stored procedure using the provided SQL statement, fetch size, and result handler.
     * The procedure is executed against the local database as the default target.
     *
     * @param statement The {@link Statement} object representing the SQL stored procedure to execute,
     *                  including the query and any parameters.
     * @param fetchSize The number of rows to fetch in each batch from the database. If set to -1,
     *                  the default fetch size is used.
     * @param consumer  A {@link Consumer} that processes the results of the executed procedure.
     *                  The results are provided as a {@link CachedRowSet} object.
     * @return A CompletableFuture that completes when the procedure execution finishes
     */
    public CompletableFuture<Void> executeProcedure(Statement statement, int fetchSize, Consumer<CachedRowSet> consumer) {
        return executeProcedure(statement, fetchSize, consumer, DEFAULT_DATABASE);
    }

    /**
     * Executes a stored procedure in the database using the provided SQL statement, fetch size, and consumer for processing the results.
     *
     * @param statement      The SQL statement containing the stored procedure call and its parameters.
     * @param fetchSize      The fetch size hint for the result set. Use -1 for the default fetch size.
     * @param consumer       The consumer to handle the processed CachedRowSet results of the procedure.
     * @param targetDatabase Specifies the target database to execute the procedure against (either LOCAL or GLOBAL).
     * @return A CompletableFuture that completes when the procedure execution finishes
     */
    public CompletableFuture<Void> executeProcedure(Statement statement, int fetchSize, Consumer<CachedRowSet> consumer, TargetDatabase targetDatabase) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
                RowSetFactory factory = RowSetProvider.newFactory();
                CachedRowSet result = factory.createCachedRowSet();

                if (fetchSize != -1) {
                    result.setFetchSize(fetchSize);
                }

                try (CallableStatement callable = connection.prepareCall(statement.getQuery())) {
                    // Set query timeout
                    callable.setQueryTimeout((int) DEFAULT_PROCEDURE_TIMEOUT_SECONDS);

                    setStatementParameters(callable, statement);
                    callable.execute();
                    result.populate(callable.getResultSet());
                    consumer.accept(result);
                    result.close();
                }
            } catch (SQLException ex) {
                log.error("Error executing procedure: {}", statement.getQuery(), ex).submit();
                throw new RuntimeException(ex); // Ensure exceptional completion
            }
        }, READ_EXECUTOR).exceptionally(ex -> {
            log.error("Unexpected error in executeProcedure for query: {}", statement.getQuery(), ex).submit();
            return null;
        });

        // Add timeout for the procedure execution
        return future.orTimeout(DEFAULT_PROCEDURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        log.error("Timeout executing procedure: {}", statement.getQuery(), ex).submit();
                    } else {
                        log.error("Error in executeProcedure: {}", statement.getQuery(), ex).submit();
                    }
                    return null;
                });
    }

    /**
     * Helper method to set parameter values on a prepared statement.
     *
     * @param preparedStatement The prepared statement to set parameters on
     * @param statement         The statement containing values to set
     * @throws SQLException If a database access error occurs
     */
    private void setStatementParameters(PreparedStatement preparedStatement, Statement statement) throws SQLException {
        int valCount = 1;
        final List<StatementValue<?>> values = statement.getValues();
        if (values == null || values.isEmpty()) {
            return;
        }
        for (StatementValue<?> val : values) {
            if (val == null) {
                preparedStatement.setObject(valCount, null);
            } else {
                preparedStatement.setObject(valCount, val.getValue(), val.getType());
            }
            valCount++;
        }
    }

    /**
     * Executes a batch of statements within a transaction context.
     *
     * @param connection The database connection
     * @param statements The statements to execute
     * @throws SQLException If a database access error occurs
     */
    private void executeTransactionalBatch(Connection connection, List<Statement> statements) throws SQLException {
        connection.setAutoCommit(false);

        try {
            int statementIndex = 0;
            for (Statement statement : statements) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {
                    // Set timeout for each statement in the batch
                    preparedStatement.setQueryTimeout((int) DEFAULT_UPDATE_TIMEOUT_SECONDS);

                    setStatementParameters(preparedStatement, statement);
                    preparedStatement.executeUpdate();
                    statementIndex++;

                } catch (SQLException e) {
                    log.error("Error executing batch statement #{} of {}: {}",
                            statementIndex + 1, statements.size(), statement.getQuery(), e).submit();
                    throw e;
                }
            }
            connection.commit(); // Commit the transaction after all statements are executed
        } catch (SQLException ex) {
            log.error("Error executing batch", ex).submit();
            connection.rollback(); // Roll back the transaction in case of any error
            throw ex; // Rethrow to ensure proper error handling in calling method
        } finally {
            connection.setAutoCommit(true); // Restore auto-commit mode
        }
    }

    /**
     * Executes statements within a transaction context, handling individual statement errors.
     *
     * @param connection The database connection
     * @param statements The statements to execute
     * @throws SQLException If a database access error occurs
     */
    private void executeTransactionalStatements(Connection connection, List<Statement> statements, boolean catchErrors) throws SQLException {
        connection.setAutoCommit(false);

        try {
            for (Statement statement : statements) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {
                    // Set timeout for each statement in the transaction
                    preparedStatement.setQueryTimeout((int) DEFAULT_UPDATE_TIMEOUT_SECONDS);

                    setStatementParameters(preparedStatement, statement);
                    preparedStatement.execute();
                } catch (SQLException ex) {
                    if (catchErrors) {
                        log.error("Error executing transaction with query: {}", statement.getQuery(), ex).submit();
                        connection.rollback();
                        throw ex; // Rethrow to trigger the outer catch block
                    }
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            if (catchErrors) {
                log.error("Error executing transaction", ex).submit();
                connection.rollback();
                throw ex; // Rethrow to ensure proper error handling in calling method
            }
        } finally {
            connection.setAutoCommit(true);
        }
    }
}