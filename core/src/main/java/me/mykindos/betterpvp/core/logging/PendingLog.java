package me.mykindos.betterpvp.core.logging;

import lombok.Data;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.items.uuiditem.UUIDItem;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
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

    /**
     * Adds the specified {@link Client} as a client context to this log for {@link LogContext#CLIENT}
     * @param player the {@link Player} to set as the {@link LogContext#CLIENT Client Context}
     * @return the {@link PendingLog}
     * @see PendingLog#addClientContext(UUID, String, boolean)
     */
    public PendingLog addClientContext(Player player) {
        return addClientContext(player.getUniqueId(), player.getName(), false);
    }

    /**
     * Adds the specified {@link Client} as a client context to this log
     * @param client the client to store as client context
     * @param target whether this is a {@link LogContext#TARGET_CLIENT} or a {@link LogContext#CLIENT}
     * @return the {@link PendingLog}
     * @see PendingLog#addClientContext(UUID, String, boolean)
     */
    public PendingLog addClientContext(Client client, boolean target) {
       return addClientContext(client.getUniqueId(), client.getName(), target);
    }

    /**
     * Adds the specified {@link Player} as a client context to this log
     * @param player the player to store as client context
     * @param target whether this is a {@link LogContext#TARGET_CLIENT} or a {@link LogContext#CLIENT}
     * @return the {@link PendingLog}
     * @see PendingLog#addClientContext(UUID, String, boolean)
     */
    public PendingLog addClientContext(Player player, boolean target) {
        return addClientContext(player.getUniqueId(), player.getName(), target);
    }

    /**
     * Adds the specified client context to this log.
     *
     * @param id the UUID of the client
     * @param name the name of the client
     * @param target whether this is a {@link LogContext#TARGET_CLIENT} or a {@link LogContext#CLIENT}
     * @return the {@link PendingLog}
     */
    public PendingLog addClientContext(UUID id, String name, boolean target) {
        if (!target) {
            context.put(LogContext.CLIENT, id.toString());
            context.put(LogContext.CLIENT_NAME, name);
        } else {
            context.put(LogContext.TARGET_CLIENT, id.toString());
            context.put(LogContext.TARGET_CLIENT_NAME, name);
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

    public PendingLog addBlockContext(BlockState blockState) {
        context.put(LogContext.BLOCK, blockState.getType().name());
        context.put(LogContext.LOCATION, UtilWorld.locationToString(blockState.getLocation(), true, true));
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
