package me.mykindos.betterpvp.champions.properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
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
        final Gamer gamer = clientManager.search().online(event.getPlayer()).getGamer();
        ChampionsProperty skillChatPreview = ChampionsProperty.SKILL_CHAT_PREVIEW;
        Optional<Boolean> skillChatPreviewOptional = gamer.getProperty(skillChatPreview);
        if (skillChatPreviewOptional.isEmpty()) {
            gamer.saveProperty(skillChatPreview, true);
        }
    }
}
