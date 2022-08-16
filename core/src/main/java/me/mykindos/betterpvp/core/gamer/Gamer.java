package me.mykindos.betterpvp.core.gamer;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.inviting.Invitable;
import me.mykindos.betterpvp.core.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilServer;

/**
 * A gamer represents a clients seasonal data.
 * Such as their blocks broken, their kills, deaths, etc.
 */
@Getter
@Setter
public class Gamer extends PropertyContainer implements Invitable {

    private final Client client;
    private final String uuid;

    public Gamer(Client client, String uuid){
        this.client = client;
        this.uuid = uuid;
    }


    private long lastDamaged;

    @Override
    public void saveProperty(String key, Object object, boolean updateScoreboard) {
        properties.put(key, object);
        UtilServer.callEvent(new GamerPropertyUpdateEvent( this, key, object, updateScoreboard));
    }

}
