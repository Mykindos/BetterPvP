package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.util.Optional;

@BPvPListener
@Singleton
public class UnbreakingRuneHandler implements Listener {

    private final UnbreakingRune unbreakingRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public UnbreakingRuneHandler(UnbreakingRune unbreakingRune, ComponentLookupService lookupService) {
        this.unbreakingRune = unbreakingRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPreDurability(PlayerItemDamageEvent event) {
        final Optional<SocketableContainerComponent> container = componentLookupService.getComponent(event.getItem(), SocketableContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        final SocketableContainerComponent runeContainer = container.get();
        if (runeContainer.hasRune(unbreakingRune)) {
            // Cancel the damage event if the Unbreaking rune is present
            event.setCancelled(true);
        }
    }
}
