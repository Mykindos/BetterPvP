package me.mykindos.betterpvp.core.logging.type.logs;

import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import org.bukkit.Location;

import java.util.UUID;

public class LocationLog {
    private Location location;
    String name;

    LocationLog(Location location, String name) {
        this.location = location;
        this.name = name;
    }

    public Statement getStatement(UUID LogUUID) {
        return new Statement("INSERT INTO loglocations (LogUUID, Name, WorldID, X, Y, Z) VALUES (?, ?, ?, ?, ?, ?)",
                new UuidStatementValue(LogUUID),
                new StringStatementValue(this.name),
                new UuidStatementValue(this.location.getWorld().getUID()),
                new IntegerStatementValue(this.location.getBlockX()),
                new IntegerStatementValue(this.location.getBlockY()),
                new IntegerStatementValue(this.location.getBlockZ()));
    }
}
