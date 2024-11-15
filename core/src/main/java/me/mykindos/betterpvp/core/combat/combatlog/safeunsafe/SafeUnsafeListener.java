package me.mykindos.betterpvp.core.combat.combatlog.safeunsafe;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.items.events.CustomPlayerItemEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

@Singleton
@BPvPListener
public class SafeUnsafeListener implements Listener {
    private final ClientManager clientManager;

    @Inject
    @Config(path = "combatlog.valuable-items", defaultValue = "TNT")
    private List<String> valuableItems;

    @Inject
    public SafeUnsafeListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    //debug logging
    @EventHandler
    public void onCustomItemEvent(final CustomPlayerItemEvent event) {
        UtilMessage.broadcast("Item", "<yellow>%s<yellow> <white>%s</white> <green>%s</green>",
                event.getPlayer().getName(),
                event.getItemStatus().name(),
                UtilItem.getItemIdentifier(event.getItem()));
    }

}
