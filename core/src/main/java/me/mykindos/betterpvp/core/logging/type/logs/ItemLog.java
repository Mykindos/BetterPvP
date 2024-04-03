package me.mykindos.betterpvp.core.logging.type.logs;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.logging.type.UUIDLogType;
import me.mykindos.betterpvp.core.logging.type.UUIDType;
import org.bukkit.Location;

import java.util.UUID;

@Getter
public class ItemLog extends SearchableLog {
    private final UUIDLogType type;
    private Statement locationStatement = null;

    public ItemLog(UUID logUUID, UUIDLogType type, UUID item) {
        super(logUUID, type.name());
        this.type = type;
        addMeta(item, UUIDType.ITEM);
    }

    public ItemLog addLocation(Location location, String name) {
        LocationLog locationLog = new LocationLog(location, name);
        locationStatement = locationLog.getStatement(this.logUUID);
        return this;
    }
}
