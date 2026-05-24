package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.diamond;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownEvent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.service.ComponentLookupService;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@BPvPListener
@Singleton
public class DiamondGemHandler implements Listener {

    private final DiamondGem diamondGem;
    private final ComponentLookupService componentLookupService;

    @Inject
    public DiamondGemHandler(DiamondGem diamondGem, ComponentLookupService componentLookupService) {
        this.diamondGem = diamondGem;
        this.componentLookupService = componentLookupService;
    }

    @EventHandler
    public void onCooldown(CooldownEvent event) {
        final Player player = event.getPlayer();
        final ItemStack mainHand = player.getInventory().getItemInMainHand();

        getSocketableContainer(mainHand).ifPresent(container -> {
            if (container.hasRune(diamondGem)) {
                double reduction = 1.0 - (diamondGem.getCooldownReduction() / 100.0);
                event.getCooldown().setSeconds((event.getCooldown().getSeconds() * reduction));
            }
        });
    }

    private Optional<SocketableContainerComponent> getSocketableContainer(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        return componentLookupService.getComponent(item, SocketableContainerComponent.class);
    }
}
