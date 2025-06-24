package me.mykindos.betterpvp.core.block.nexo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureBreakEvent;
import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractionService;
import me.mykindos.betterpvp.core.block.data.SmartBlockData;
import me.mykindos.betterpvp.core.block.data.storage.StorageBlockData;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Singleton
public class NexoSmartBlockInteractionService implements SmartBlockInteractionService, Listener {

    private final NexoSmartBlockFactory blockFactory;

    @Inject
    private NexoSmartBlockInteractionService(Core core, NexoSmartBlockFactory blockFactory) {
        this.blockFactory = blockFactory;
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

    // Clear storage pdc and drop items on break
    @EventHandler
    public void onBreak(NexoFurnitureBreakEvent event) {
        final Optional<SmartBlockInstance> from = blockFactory.from(event.getBaseEntity());

        if (from.isEmpty()) {
            return;
        }

        final SmartBlockInstance instance = from.get();
        final SmartBlockData<StorageBlockData> data = instance.getBlockData();
        data.update(storage -> {
            final List<@NotNull ItemInstance> content = storage.getContent();
            content.clear();

            // Drop items
            final Location centerLocation = instance.getHandle().getLocation().toCenterLocation();
            for (ItemInstance item : content) {
                final ItemStack itemStack = item.createItemStack();
                instance.getHandle().getWorld().dropItemNaturally(centerLocation, itemStack);
            }
        });
    }
}
