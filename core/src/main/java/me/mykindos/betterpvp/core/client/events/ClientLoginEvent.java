package me.mykindos.betterpvp.core.client.events;


import lombok.EqualsAndHashCode;
import lombok.Value;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Value
public class ClientLoginEvent extends CustomEvent {

    Client client;
    Player player;

}
