package me.mykindos.betterpvp.core.logging;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

@Data
public class PendingLog {

    private final UUID id;
    private final String className;
    private final String level;
    private final String message;
    private final long time;
    private final Object[] args;
    private String action;
    private HashMap<String, String> context = new HashMap<>();

    public PendingLog setAction(String action) {
        this.action = action;
        return this;
    }

    public PendingLog addContext(String key, String value) {
        context.put(key, value);
        return this;
    }

    public PendingLog addClientContext(Player player) {
        context.put(LogContext.CLIENT, player.getUniqueId().toString());
        context.put(LogContext.CLIENT_NAME, player.getName());
        return this;
    }

    public PendingLog addClientContext(Client client, boolean target) {
        if (!target) {
            context.put(LogContext.CLIENT, client.getUuid());
            context.put(LogContext.CLIENT_NAME, client.getName());
        } else {
            context.put(LogContext.TARGET_CLIENT, client.getUuid());
            context.put(LogContext.TARGET_CLIENT_NAME, client.getName());
        }
        return this;
    }

    public PendingLog addClientContext(Player player, boolean target) {
        if (!target) {
            context.put(LogContext.CLIENT, player.getUniqueId().toString());
            context.put(LogContext.CLIENT_NAME, player.getName());
        } else {
            context.put(LogContext.TARGET_CLIENT, player.getUniqueId().toString());
            context.put(LogContext.TARGET_CLIENT_NAME, player.getName());
        }
        return this;
    }

    public PendingLog addClanContext(IClan clan) {
        return addClanContext(clan, false);
    }

    public PendingLog addClanContext(IClan clan, boolean target) {
        if (!target) {
            context.put(LogContext.CLAN, clan.getId().toString());
            context.put(LogContext.CLAN_NAME, clan.getName());
        } else {
            context.put(LogContext.TARGET_CLAN, clan.getId().toString());
            context.put(LogContext.TARGET_CLAN_NAME, clan.getName());
        }
        return this;
    }

    public PendingLog addLocationContext(Location location) {
        context.put(LogContext.LOCATION, UtilWorld.locationToString(location, true, true));
        return this;
    }

    public PendingLog addBlockContext(Block block) {
        context.put(LogContext.BLOCK, block.getType().name());
        context.put(LogContext.LOCATION, UtilWorld.locationToString(block.getLocation(), true, true));
        return this;
    }

    public PendingLog addItemContext(UUIDItem item) {
        context.put(LogContext.ITEM, item.getUuid().toString());
        context.put(LogContext.ITEM_NAME, item.getIdentifier());
        return this;
    }

    public void submit() {
        LoggerFactory.getInstance().addLog(this);
    }

}
