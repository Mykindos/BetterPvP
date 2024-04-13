package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageItem(PlayerItemDamageEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(damageCustomItem(event.getPlayer(), event.getItem(), 1));
    }

    /**
     * @param player    the player that the ItemStack is on
     * @param itemStack the itemStack to try and damage
     * @param damage    the amount of damage to apply
     * @return true if the damage was processed (it was a custom item with durability), false if not
     */
    public boolean damageCustomItem(Player player, ItemStack itemStack, int damage) {
        BPvPItem item = itemHandler.getItem(itemStack);
        if (item != null && item.getMaxDurability() >= 0) {
            item.damageItem(player, itemStack, damage);
            return true;
        }
        return false;
    }


}
