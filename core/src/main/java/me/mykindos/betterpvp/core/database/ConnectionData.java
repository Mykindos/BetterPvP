package me.mykindos.betterpvp.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;

import java.sql.Connection;

@AllArgsConstructor
@Getter
public class ConnectionData {

    private final HikariConfig hikariConfig;
    private final HikariDataSource dataSource;

    @SneakyThrows
    public Connection getConnection() {
        return dataSource.getConnection();
    }

}
