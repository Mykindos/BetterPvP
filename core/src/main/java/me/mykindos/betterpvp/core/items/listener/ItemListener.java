package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

@CustomLog
@BPvPListener
public class ItemListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public ItemListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onImmuneDamage(PlayerItemDamageEvent event) {
        BPvPItem item = itemHandler.getItem(event.getItem());
        if (item != null && !item.hasDurability()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageItem(PlayerItemDamageEvent event) {
        itemHandler.updateNames(event.getItem());
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void craftItem(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        final ItemStack result = event.getRecipe().getResult();
        event.getInventory().setResult(itemHandler.updateNames(result));
    }

}
