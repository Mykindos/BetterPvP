package me.mykindos.betterpvp.core.item.component.impl.durability;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
@BPvPListener
@Singleton
public class DurabilityHandler implements Listener {

    private final ItemFactory itemFactory;

    @Inject
    private DurabilityHandler(ItemFactory itemFactory) {
        this.itemFactory = itemFactory;

        // If items have a durability component, we want to add a Minecraft
        // damage component so they can be used with vanilla mechanics
        // and the PlayerItemDamageEvent is called, even if we don't use the
        // component as intended.
        itemFactory.registerDefaultBuilder(instance -> {
            final Optional<DurabilityComponent> componentOpt = instance.getComponent(DurabilityComponent.class);
            if (componentOpt.isEmpty()) {
                return; // No durability component present
            }

            final ItemStack itemStack = instance.getItemStack();
            itemStack.setData(DataComponentTypes.MAX_DAMAGE, 100);
            itemStack.setData(DataComponentTypes.DAMAGE, 0);
        });
    }

    // We cancel it because we want to handle it ourselves
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDamage(PlayerItemDamageEvent event) {
        if (event.isCancelled()) {
            return; // If the event is already cancelled, we do nothing
        }

        final int damage = event.getDamage();
        event.setDamage(0);
        final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(event.getItem());
        if (instanceOpt.isEmpty()) {
            return; // No item instance present
        }

        final ItemInstance instance = instanceOpt.get();
        final Optional<DurabilityComponent> opt = instance.getComponent(DurabilityComponent.class);
        if (opt.isEmpty()) {
            return; // No durability component present
        }

        final DurabilityComponent component = opt.get();
        // Call the event
        final ItemDamageEvent itemDamageEvent = new ItemDamageEvent(instance, component, damage);
        if (!itemDamageEvent.isCancelled() && itemDamageEvent.willBreak()) {
            UtilItem.breakItem(event.getPlayer(), event.getItem());
            new ItemBreakEvent(instance, component).callEvent();
        } else {
            component.setDamage(component.getDamage() + damage);
            instance.serializeAllComponentsToItemStack();
        }
    }
}
