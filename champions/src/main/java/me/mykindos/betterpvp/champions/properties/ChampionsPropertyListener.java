package me.mykindos.betterpvp.champions.properties;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientLoginEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;


@Singleton
@BPvPListener
public class ChampionsPropertyListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public ChampionsPropertyListener(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onPropertyUpdated(ChampionsPropertyUpdateEvent event) {
        gamerManager.getGamerRepository().saveProperty(event.getGamer(), event.getProperty(), event.getValue());
    }

    @EventHandler
    public void onJoin(ClientLoginEvent event) {
        gamerManager.getObject(event.getClient().getUuid()).ifPresent(gamer -> {
            ChampionsProperty skillChatPreview = ChampionsProperty.SKILL_CHAT_PREVIEW;
            Optional<Boolean> skillChatPreviewOptional = gamer.getProperty(skillChatPreview);
            if (skillChatPreviewOptional.isEmpty()) {
                gamer.saveProperty(skillChatPreview, true);
            }
        });
    }
}
