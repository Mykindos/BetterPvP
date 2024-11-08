package me.mykindos.betterpvp.core.database;

import com.mineplex.studio.sdk.modules.manageddb.ManagedDBModule;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.core.Core;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Represents a single-source of data for the Mineplex server.
 */
@AllArgsConstructor
@Getter
@CustomLog
public final class MineplexDataSource implements DataSource {

    private final Core core;
    private final ManagedDBModule module;
    private final String name;

    @SneakyThrows
    @Override
    public Connection getConnection() {
        return module.openManagedMySQLConnection(name);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("Creating new connections is not supported.");
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException("Logging is not supported.");
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException("Logging is not supported.");
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new SQLFeatureNotSupportedException("Login timeout is not supported.");
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new SQLFeatureNotSupportedException("Login timeout is not supported.");
    }

    @Override
    public Logger getParentLogger() {
        return core.getLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (this.isWrapperFor(iface)) {
            return iface.cast(this);
        } else {
            throw new SQLException("Cannot unwrap to " + iface.getName());
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
}
