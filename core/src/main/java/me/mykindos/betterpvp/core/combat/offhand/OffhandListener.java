package me.mykindos.betterpvp.core.combat.offhand;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.AsyncClientLoadEvent;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.display.TimedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import java.util.Objects;
import java.util.Optional;

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
        event.getClient().getGamer().setOffhandExecutor(offhandController.getDefaultExecutor());
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
        final OffhandExecutor executor = gamer.getOffhandExecutor();
        if (executor == null) {
            actionBar(gamer);
            return; // No offhand executor, nothing to do.
        }

        final Optional<ItemInstance> item = itemFactory.fromItemStack(event.getOffHandItem());
        if (item.isEmpty()) {
            actionBar(gamer);
            return; // No item in the main hand, nothing to do.
        }

        if (!executor.trigger(client, item.get())) {
            actionBar(gamer);
        }
    }

    private void actionBar(Gamer gamer) {
        gamer.getActionBar().add(4, new TimedComponent(
                2.0,
                false,
                gmr -> Component.text("No action to trigger", NamedTextColor.RED)
        ));
        final Player player = Objects.requireNonNull(gamer.getPlayer());
        new SoundEffect(Sound.UI_BUTTON_CLICK, 0.8f, 1f).play(player);
    }

}
