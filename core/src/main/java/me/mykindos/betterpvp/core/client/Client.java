package me.mykindos.betterpvp.core.client;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.properties.ClientPropertyUpdateEvent;
import me.mykindos.betterpvp.core.properties.PropertyContainer;
import me.mykindos.betterpvp.core.utilities.UtilServer;

@Setter
@Getter
@Builder
public class Client extends PropertyContainer {

    String uuid;
    String name;
    @Builder.Default
    Rank rank = Rank.PLAYER;

    boolean administrating;

    public boolean hasRank(Rank rank) {
        return this.rank.getId() >= rank.getId();
    }

    @Override
    public void saveProperty(String key, Object object, boolean updateScoreboard) {
        properties.put(key, object);
        UtilServer.callEvent(new ClientPropertyUpdateEvent(this, key, object, updateScoreboard));
    }

}
