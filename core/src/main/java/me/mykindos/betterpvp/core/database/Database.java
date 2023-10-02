package me.mykindos.betterpvp.core.database;

import com.google.inject.Singleton;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.connection.IDatabaseConnection;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.StatementValue;
import me.mykindos.betterpvp.core.utilities.UtilServer;

import javax.inject.Inject;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetFactory;
import javax.sql.rowset.RowSetProvider;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Singleton
public class Database {

    @Getter
    private final Core core;

    @Getter
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
        UtilServer.runTaskAsync(core, () -> executeUpdate(statement));

    }

    /**
     * @param statement The statement and values
     */
    public void executeUpdate(Statement statement) {
        Connection connection = getConnection().getDatabaseConnection();
        try {
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery());
            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                preparedStatement.setObject(i, val.getValue(), val.getType());
            }
            preparedStatement.executeUpdate();

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void executeBatch(List<Statement> statements, boolean async) {
        if (statements.isEmpty()) {
            return;
        }

        if(async) {
            UtilServer.runTaskAsync(core, () -> executeBatch(statements));
        }else{
            executeBatch(statements);
        }
    }

    private void executeBatch(List<Statement> statements) {
        Connection connection = getConnection().getDatabaseConnection();
        if (statements.isEmpty()) {
            return;
        }
        try {
            connection.setAutoCommit(false);
            // Assume all statement queries are the same
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statements.get(0).getQuery());
            for (Statement statement : statements) {
                for (int i = 0; i < statement.getValues().length; i++) {
                    StatementValue<?> val = statement.getValues()[i];
                    preparedStatement.setObject(i, val.getValue(), val.getType());
                }
                preparedStatement.addBatch(statement.getQuery());
            }
            preparedStatement.executeBatch();
            connection.commit();
        } catch (SQLException ex) {
            ex.printStackTrace();
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param statement The statement and values
     */
    public CachedRowSet executeQuery(Statement statement) {
        Connection connection = getConnection().getDatabaseConnection();
        CachedRowSet rowset = null;

        try {
            RowSetFactory factory = RowSetProvider.newFactory();
            rowset = factory.createCachedRowSet();
            rowset.setFetchSize(100);
            @Cleanup
            PreparedStatement preparedStatement = connection.prepareStatement(statement.getQuery());
            for (int i = 1; i <= statement.getValues().length; i++) {
                StatementValue<?> val = statement.getValues()[i - 1];
                preparedStatement.setObject(i, val.getValue(), val.getType());
            }
            rowset.populate(preparedStatement.executeQuery());

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return rowset;
    }

}
