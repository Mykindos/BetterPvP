package me.mykindos.betterpvp.core.block.oraxen;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureBreakEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureDamageEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurnitureInteractEvent;
import io.th0rgal.oraxen.api.events.furniture.OraxenFurniturePlaceEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

@Singleton
public class OraxenSmartBlockInteractionService implements SmartBlockInteractionService, Listener {

    private final OraxenSmartBlockFactory blockFactory;
    private final SmartBlockDataManager dataManager;

    @Inject
    private OraxenSmartBlockInteractionService(Core core, OraxenSmartBlockFactory blockFactory, SmartBlockDataManager dataManager) {
        this.blockFactory = blockFactory;
        this.dataManager = dataManager;
        Bukkit.getPluginManager().registerEvents(this, core);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInteract(OraxenFurnitureInteractEvent event) {
        if (!event.getHand().equals(EquipmentSlot.HAND)) {
            return;
        }

        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        if (instance.getType().handleClick(instance, event.getPlayer(), Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onDamage(OraxenFurnitureDamageEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        if (instance.getType().handleClick(instance, event.getPlayer(), Action.LEFT_CLICK_BLOCK)) {
            event.setCancelled(true); // Prevent default damage interaction if not handled
        }
    }

    @EventHandler
    void onBreak(OraxenFurnitureBreakEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();

        dataManager.removeData(instance, BlockRemovalCause.NATURAL);
    }

    @EventHandler
    void onPlace(OraxenFurniturePlaceEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        dataManager.getProvider().getOrCreateData(instance); // this saves
    }
}
