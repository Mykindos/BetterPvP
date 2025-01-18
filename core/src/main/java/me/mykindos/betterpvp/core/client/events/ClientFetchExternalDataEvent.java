package me.mykindos.betterpvp.core.client.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

import java.util.HashMap;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClientFetchExternalDataEvent extends CustomEvent {

    private final Client client;
    private HashMap<String, Object> data = new HashMap<>();

}
