package me.mykindos.betterpvp.core.client.events;


import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

@EqualsAndHashCode(callSuper = true)
@Data
public class ClientQuitEvent extends CustomCancellableEvent {

    private final Client client;
    private final Player player;
    private Component quitMessage;

}
