package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.hookmaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class HookmasterRuneHandler implements Listener {

    private final HookmasterRune hookmasterRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public HookmasterRuneHandler(HookmasterRune hookmasterRune, ComponentLookupService lookupService) {
        this.hookmasterRune = hookmasterRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEquip(PlayerProgressionExperienceEvent event) {
        if (!event.getProfession().equalsIgnoreCase("Fishing")) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack hand = player.getEquipment().getItemInMainHand();
        final Optional<SocketableContainerComponent> container = componentLookupService.getComponent(hand, SocketableContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        final SocketableContainerComponent runeContainer = container.get();
        if (runeContainer.hasRune(hookmasterRune)) {
            event.setGainedExp(event.getGainedExp() * (1 + hookmasterRune.getPercent()));
        }
    }
}
