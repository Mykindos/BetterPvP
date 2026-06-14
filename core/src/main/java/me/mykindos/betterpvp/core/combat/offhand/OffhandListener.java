package me.mykindos.betterpvp.core.combat.offhand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

@BPvPListener
@Singleton
public class OffhandListener implements Listener {

    private final OffhandController offhandController;
    private final ClientManager clientManager;
    private final ItemFactory itemFactory;

    @Inject
    public OffhandListener(OffhandController offhandController, ClientManager clientManager, ItemFactory itemFactory) {
        this.offhandController = offhandController;
        this.clientManager = clientManager;
        this.itemFactory = itemFactory;
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        final OffhandExecutor defaultExecutor = offhandController.getDefaultExecutor();
        if (defaultExecutor != null) {
            // Fallback slot, below the per-feature executors registered elsewhere.
            event.getClient().getGamer().setOffhandExecutor(-1, defaultExecutor);
        }
    }

    /**
     * No hand swapping!
     *
     * @param event the event
     */
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        event.setCancelled(true);

        final Client client = clientManager.search().online(event.getPlayer());
        final Gamer gamer = client.getGamer();

        final ItemInstance item = itemFactory.fromItemStack(event.getOffHandItem()).orElse(null);
        for (OffhandExecutor executor : gamer.getOffhandExecutors().values()) {
            if (executor.trigger(client, item)) {
                return;
            }
        }
    }
}
