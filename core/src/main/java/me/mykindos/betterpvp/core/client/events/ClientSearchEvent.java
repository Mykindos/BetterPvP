package me.mykindos.betterpvp.core.client.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClientSearchEvent extends CustomEvent {

    private final Client client;
    private final HashMap<String, Object> additionalData = new HashMap<>();

    public ClientSearchEvent(Client client) {
        super(true);
        this.client = client;
    }
}
