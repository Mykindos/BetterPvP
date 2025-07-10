package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurniturePlaceEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.data.BlockRemovalCause;
import me.mykindos.betterpvp.core.block.data.SmartBlockDataManager;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Optional<SmartBlockInstance> from = blockFactory.from(event.getRightClicked());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        if (instance.getType().handleClick(instance, event.getPlayer(), Action.RIGHT_CLICK_BLOCK)) {
            event.setCancelled(true); // Prevent default interaction if not handled
        }
    }

    @EventHandler
    public void onInteractBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getClickedBlock() == null) {
            return;
        }

        final Optional<SmartBlockInstance> from = blockFactory.from(event.getClickedBlock());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        if (instance.getType().handleClick(instance, event.getPlayer(), event.getAction())) {
            event.setCancelled(true); // Prevent default interaction if not handled
        }
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
