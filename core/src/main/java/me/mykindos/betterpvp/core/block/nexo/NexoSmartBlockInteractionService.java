package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureDamageEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.manager.SmartBlockDataManager;
import me.mykindos.betterpvp.core.item.component.impl.ability.event.PlayerItemAbilityEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import java.util.Optional;

@Singleton
public class NexoSmartBlockInteractionService implements SmartBlockInteractionService, Listener {

    private final NexoSmartBlockFactory blockFactory;
    private final SmartBlockDataManager dataManager;

    @Inject
    private NexoSmartBlockInteractionService(Core core, NexoSmartBlockFactory blockFactory, SmartBlockDataManager dataManager) {
        this.blockFactory = blockFactory;
        this.dataManager = dataManager;
        Bukkit.getPluginManager().registerEvents(this, core);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInteract(NexoFurnitureInteractEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        if (instance.getType().handleClick(instance, event.getPlayer(), Action.RIGHT_CLICK_BLOCK)) {
            event.setUseFurniture(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onDamage(NexoFurnitureDamageEvent event) {
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
    void onBreak(NexoFurnitureBreakEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();

        dataManager.removeData(instance, BlockRemovalCause.NATURAL);
    }

    @EventHandler
    void onPlace(NexoFurniturePlaceEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        dataManager.getProvider().getOrCreateData(instance); // this saves
    }
}
