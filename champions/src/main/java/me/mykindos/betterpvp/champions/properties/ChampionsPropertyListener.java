package me.mykindos.betterpvp.champions.properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;


@Singleton
@BPvPListener
public class ChampionsPropertyListener implements Listener {

    private final ClientManager clientManager;

    @Inject
    public ChampionsPropertyListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onPropertyUpdated(ChampionsPropertyUpdateEvent event) {
        clientManager.saveGamerProperty(event.getGamer(), event.getProperty(), event.getValue());
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());
        ChampionsProperty skillChatPreview = ChampionsProperty.SKILL_CHAT_PREVIEW;
        Optional<Boolean> skillChatPreviewOptional = client.getProperty(skillChatPreview);
        if (skillChatPreviewOptional.isEmpty()) {
            client.saveProperty(skillChatPreview, true);
        }

        Optional<Boolean> skillWeaponTooltip = client.getProperty(ChampionsProperty.SKILL_WEAPON_TOOLTIP);
        if (skillWeaponTooltip.isEmpty()) {
            client.saveProperty(ChampionsProperty.SKILL_WEAPON_TOOLTIP, true);
        }
    }
}
