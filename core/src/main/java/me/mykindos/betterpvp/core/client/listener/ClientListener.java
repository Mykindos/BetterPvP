package me.mykindos.betterpvp.core.client.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Optional;

@BPvPListener
public record ClientListener(ClientManager clientManager) implements Listener {

    @Inject
    public ClientListener {
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        Optional<Client> clientOptional = clientManager.getObject(uuid);
        if (clientOptional.isEmpty()) {
            Client client = new Client(uuid);
            clientManager.addObject(uuid, client);
            clientManager.getRepository().save(client);
        }
    }

}
