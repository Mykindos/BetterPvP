package me.mykindos.betterpvp.core.client;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Setter
@Getter
public class Client extends PropertyContainer implements IMapListener {

    private String uuid;
    private String name;
    private Rank rank;

    boolean administrating;

    public Client(String uuid, String name, Rank rank) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
        properties.registerListener(this);
    }

    public boolean hasRank(Rank rank) {
        return this.rank.getId() >= rank.getId();
    }

    @Override
    public void saveProperty(String key, Object object, boolean updateScoreboard) {
        properties.put(key, object);
        if (updateScoreboard) {
            Player player = Bukkit.getPlayer(UUID.fromString(getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        }

    }

    @Override
    public void onMapValueChanged(String key, Object value) {
        UtilServer.callEvent(new ClientPropertyUpdateEvent(this, key, value));
    }
}
