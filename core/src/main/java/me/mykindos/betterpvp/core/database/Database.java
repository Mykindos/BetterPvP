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
import me.mykindos.betterpvp.core.utilities.UtilServer;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

@Getter
@CustomLog
@Singleton
public class Database {

    private final Core core;

    private final IDatabaseConnection connection;

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
        UtilServer.runTaskAsync(core, () -> executeUpdate(statement, targetDatabase));
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

            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                preparedStatement.setObject(i, val.getValue(), val.getType());
            }
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException ex) {
            log.error("Error executing update: {}", statement.getQuery(), ex).submit();
        }
    }

    public void executeBatch(List<Statement> statements, boolean async) {
        executeBatch(statements, async, null, TargetDatabase.LOCAL);
    }

    public void executeBatch(List<Statement> statements, boolean async, TargetDatabase targetDatabase) {
        executeBatch(statements, async, null, targetDatabase);
    }

    public void executeBatch(List<Statement> statements, boolean async, Consumer<ResultSet> callback) {
        executeBatch(statements, async, callback, TargetDatabase.LOCAL);
    }

    public void executeBatch(List<Statement> statements, boolean async, Consumer<ResultSet> callback, TargetDatabase targetDatabase) {
        if (async) {
            UtilServer.runTaskAsync(core, () -> executeBatch(statements, callback, targetDatabase));
        } else {
            executeBatch(statements, callback, targetDatabase);
        }
    }

    private void executeBatch(List<Statement> statements, Consumer<ResultSet> callback) {
        executeBatch(statements, callback, TargetDatabase.LOCAL);
    }

    private void executeBatch(List<Statement> statements, Consumer<ResultSet> callback, TargetDatabase targetDatabase) {
        if(statements.isEmpty()) {
            return;
        }

        try (Connection connection = getConnection().getDatabaseConnection(targetDatabase)) {
            connection.setAutoCommit(false);
            try (PreparedStatement preparedStatement = connection.prepareStatement(statements.get(0).getQuery())) {
                for (Statement statement : statements) {
                    for (int i = 1; i <= statement.getValues().length; i++) {
                        StatementValue<?> val = statement.getValues()[i - 1];
                        preparedStatement.setObject(i, val.getValue(), val.getType());
                    }
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();

                if (callback != null) {
                    callback.accept(preparedStatement.getGeneratedKeys());
                }

            } catch (SQLException ex) {
                log.error("Error executing batch", ex).submit();
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
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
            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                preparedStatement.setObject(i, val.getValue(), val.getType());
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
                for (int i = 1; i <= statement.getValues().length; i++) {
                    StatementValue<?> val = statement.getValues()[i - 1];
                    callable.setObject(i, val.getValue(), val.getType());
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