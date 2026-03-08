package me.mykindos.betterpvp.core.item.component.impl.runes.forestwright;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.components.professions.PlayerProgressionExperienceEvent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
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
public class ForestwrightRuneHandler implements Listener {

    private final ForestwrightRune forestwrightRune;
    private final ComponentLookupService componentLookupService;

    @Inject
    public ForestwrightRuneHandler(ForestwrightRune forestwrightRune, ComponentLookupService lookupService) {
        this.forestwrightRune = forestwrightRune;
        this.componentLookupService = lookupService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEquip(PlayerProgressionExperienceEvent event) {
        if (!event.getProfession().equalsIgnoreCase("Woodcutting")) {
            return;
        }

        final Player player = event.getPlayer();
        final ItemStack hand = player.getEquipment().getItemInMainHand();
        final Optional<RuneContainerComponent> container = componentLookupService.getComponent(hand, RuneContainerComponent.class);
        if (container.isEmpty()) {
            return; // No runes present
        }

        final RuneContainerComponent runeContainer = container.get();
        if (runeContainer.hasRune(forestwrightRune)) {
            event.setGainedExp(event.getGainedExp() * (1 + forestwrightRune.getPercent()));
        }
    }
}
