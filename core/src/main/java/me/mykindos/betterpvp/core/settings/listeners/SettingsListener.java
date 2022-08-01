package me.mykindos.betterpvp.core.settings.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.events.ButtonPostClickEvent;
import me.mykindos.betterpvp.core.settings.events.SettingsUpdatedEvent;
import me.mykindos.betterpvp.core.settings.menus.buttons.SettingsButton;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
public class SettingsListener implements Listener {

    private final ClientManager clientManager;

    @Inject
    public SettingsListener(ClientManager clientManager){
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onSettingButtonClick(ButtonPostClickEvent event){
        if(event.getButton() instanceof SettingsButton settingsButton) {
            Optional<Client> clientOptional = clientManager.getObject(event.getPlayer().getUniqueId());
            clientOptional.ifPresent(client -> {
                UtilServer.callEvent(new SettingsUpdatedEvent(event.getPlayer(), client, settingsButton.getSetting()));
            });

        }
    }
}
