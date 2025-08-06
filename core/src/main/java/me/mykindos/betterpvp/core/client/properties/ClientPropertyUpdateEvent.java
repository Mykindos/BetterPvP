package me.mykindos.betterpvp.core.client.properties;

import lombok.Getter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.properties.PropertyUpdateEvent;

@Getter
public class ClientPropertyUpdateEvent extends PropertyUpdateEvent<Client> {

    public ClientPropertyUpdateEvent(Client container, String property, Object newValue, Object oldValue) {
        super(container, property, newValue, oldValue);
    }
}
