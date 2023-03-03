package me.mykindos.betterpvp.core.client.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class ClientPropertyUpdateEvent extends PropertyUpdateEvent {

    private final Client client;

    public ClientPropertyUpdateEvent(Client client, String property, Object object) {
        super(property, object);
        this.client = client;
    }
}
