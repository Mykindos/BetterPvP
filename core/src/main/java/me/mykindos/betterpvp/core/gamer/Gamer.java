package me.mykindos.betterpvp.core.gamer;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.customtypes.IMapListener;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A gamer represents a clients seasonal data.
 * Such as their blocks broken, their kills, deaths, etc.
 */
@Getter
@Setter
public class Gamer extends PropertyContainer implements Invitable, IMapListener {

    private final Client client;
    private final String uuid;

    private long lastDamaged;

    public Gamer(Client client, String uuid){
        this.client = client;
        this.uuid = uuid;
        properties.registerListener(this);
    }

    public int getBalance() {
        return (int) getProperty(GamerProperty.BALANCE).orElse(0);
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
        UtilServer.callEvent(new GamerPropertyUpdateEvent( this, key, value));
    }

}
