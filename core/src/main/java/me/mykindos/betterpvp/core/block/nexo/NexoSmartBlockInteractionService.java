package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

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

    @EventHandler
    public void onInteract(NexoFurnitureInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        instance.getType().getClickBehavior().ifPresent(behavior -> {
            behavior.trigger(instance, event.getPlayer());
        });
    }

    @EventHandler
    public void onBreak(NexoFurnitureBreakEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();

        dataManager.removeData(instance, BlockRemovalCause.NATURAL);
    }

    @EventHandler
    public void onPlace(NexoFurniturePlaceEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        dataManager.getOrCreateData(instance); // this saves
    }
}
