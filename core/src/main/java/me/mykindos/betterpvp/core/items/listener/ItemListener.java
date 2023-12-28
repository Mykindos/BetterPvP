package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

@Slf4j
@BPvPListener
public class ItemListener implements Listener {

    private final ItemHandler itemHandler;

    @Inject
    public ItemListener(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onDamageItem(PlayerItemDamageEvent event) {
        if (event.isCancelled()) return;
        ItemStack itemStack = event.getItem();
        BPVPItem item = itemHandler.getItem(itemStack);
        if (item != null && item.getMaxDurability() >= 0) {
            itemHandler.damageItem(event.getPlayer(), itemStack, item, event.getDamage());
            event.setDamage(0);
            event.setCancelled(true);
        }
    }

    public void onBreakBlockEvent(BlockBreakEvent event) {

    }
}
