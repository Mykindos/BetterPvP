package me.mykindos.betterpvp.core.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Cleanup;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.StatementValue;

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
import java.util.function.Consumer;

@Getter
@CustomLog
@Singleton
public class Database {

    private final Core core;

    private final IDatabaseConnection connection;
    private static final Executor QUERY_EXECUTOR = Executors.newSingleThreadExecutor();

    @Inject
    public Database(Core core, IDatabaseConnection connection) {
        this.core = core;
        this.connection = connection;
    }

    /**
     * @param statement The statement and values
     */
    public void executeUpdateAsync(Statement statement) {
        executeUpdateAsync(statement, TargetDatabase.LOCAL);
    }

    /**
     * @param statement The statement and values
     */
    public void executeUpdateAsync(Statement statement, TargetDatabase targetDatabase) {
        CompletableFuture.runAsync(() -> executeUpdate(statement, targetDatabase), QUERY_EXECUTOR);
    }

    public void executeUpdate(Statement statement) {
        executeUpdate(statement, TargetDatabase.LOCAL);
    }

    /**
     * @param statement The statement and values
     */
    public void executeUpdate(Statement statement, TargetDatabase targetDatabase) {

        try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {

            PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery());

            int valCount = 1;
            for(StatementValue<?> val : statement.getValues()) {
                preparedStatement.setObject(valCount, val.getValue(), val.getType());
                valCount++;
            }

            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException ex) {
            log.error("Error executing update: {}", statement.getQuery(), ex).submit();
        }
    }

    public void executeBatch(List<Statement> statements, boolean async) {
        executeBatch(statements, async, TargetDatabase.LOCAL);
    }

    public void executeBatch(List<Statement> statements, boolean async, TargetDatabase targetDatabase) {
        CompletableFuture.runAsync(() -> {
            if(statements.isEmpty()) {
                return;
            }

            try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
                connection.setAutoCommit(false);

                try {
                    for (Statement statement : statements) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {
                            int valCount = 1;
                            for(StatementValue<?> val : statement.getValues()) {
                                preparedStatement.setObject(valCount, val.getValue(), val.getType());
                                valCount++;
                            }
                            preparedStatement.executeUpdate();
                        }
                    }

                    connection.commit(); // Commit the transaction after all statements are executed

                } catch (SQLException ex) {
                    log.error("Error executing batch", ex).submit();
                    connection.rollback(); // Roll back the transaction in case of any error
                } finally {
                    connection.setAutoCommit(true); // Restore auto-commit mode
                }
            } catch (SQLException e) {
                log.error("Failed to manage transaction or close connection", e).submit();
            }
        }).exceptionally(ex -> {
            log.error("Error executing batch", ex).submit();
            return null;
        });

    }


    public void executeTransaction(List<Statement> statements, boolean async, TargetDatabase targetDatabase) {
        if (async) {
            CompletableFuture.runAsync(() -> executeTransaction(statements, targetDatabase), QUERY_EXECUTOR);
        } else {
            executeTransaction(statements, targetDatabase);
        }
    }

    public void executeTransaction(List<Statement> statements, boolean async) {
        executeTransaction(statements, async, TargetDatabase.LOCAL);
    }

    private void executeTransaction(List<Statement> statements, TargetDatabase targetDatabase) {
        if (statements.isEmpty()) {
            return;
        }

        try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
            connection.setAutoCommit(false);

            for (Statement statement : statements) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery())) {
                    int valCount = 1;
                    for(StatementValue<?> val : statement.getValues()) {
                        preparedStatement.setObject(valCount, val.getValue(), val.getType());
                        valCount++;
                    }
                    preparedStatement.execute();
                } catch (SQLException ex) {
                    log.error("Error executing transaction", ex).submit();
                    connection.rollback();
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            log.error("Failed to manage transaction or close connection", e).submit();
        }
    }

    public CachedRowSet executeQuery(Statement statement) {
        return executeQuery(statement, TargetDatabase.LOCAL);
    }

    /**
     * @param statement The statement and values
     */
    public CachedRowSet executeQuery(Statement statement, TargetDatabase targetDatabase) {

        CachedRowSet rowset = null;

        try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
            RowSetFactory factory = RowSetProvider.newFactory();
            rowset = factory.createCachedRowSet();
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery());
            int valCount = 1;
            for(StatementValue<?> val : statement.getValues()) {
                preparedStatement.setObject(valCount, val.getValue(), val.getType());
                valCount++;
            }
            rowset.populate(preparedStatement.executeQuery());
            preparedStatement.close();

        } catch (SQLException ex) {
            log.error("Error executing query: {}", statement.getQuery(), ex).submit();
        }

        return rowset;
    }

    @SneakyThrows
    public void executeProcedure(Statement statement, int fetchSize, Consumer<CachedRowSet> consumer) {
        executeProcedure(statement, fetchSize, consumer, TargetDatabase.LOCAL);
    }

    @SneakyThrows
    public void executeProcedure(Statement statement, int fetchSize, Consumer<CachedRowSet> consumer, TargetDatabase targetDatabase) {

        CachedRowSet result;

        try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
            RowSetFactory factory = RowSetProvider.newFactory();
            result = factory.createCachedRowSet();
            if (fetchSize != -1) result.setFetchSize(fetchSize);
            try (CallableStatement callable = connection.prepareCall(statement.getQuery())) {
                int valCount = 1;
                for(StatementValue<?> val : statement.getValues()) {
                    callable.setObject(valCount, val.getValue(), val.getType());
                    valCount++;
                }
                callable.execute();
                result.populate(callable.getResultSet());
                consumer.accept(result);
                result.close();
            }

        } catch (SQLException ex) {
            log.info("Error executing procedure: {}", statement.getQuery(), ex).submit();
        }
    }

}