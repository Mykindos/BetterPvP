package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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
        itemHandler.updateNames(event.getItem());
    }

    /**
     * @param player    the player that the ItemStack is on
     * @param itemStack the itemStack to try and damage
     * @param damage    the amount of damage to apply
     * @return true if the damage was processed (it was a custom item with durability), false if not
     */
    public boolean damageItem(Player player, ItemStack itemStack, int damage) {
        PlayerItemDamageEvent playerItemDamageEvent = UtilServer.callEvent(new PlayerItemDamageEvent(player, itemStack, damage, damage));
        ItemMeta itemMeta = playerItemDamageEvent.getItem().getItemMeta();
        if (itemMeta instanceof Damageable damageable) {
            if (damageable.hasMaxDamage()) {
                int currentDamage = damageable.hasDamageValue() ? damageable.getDamage() : 0;
                int newDamage = currentDamage + playerItemDamageEvent.getDamage();
                if (newDamage > damageable.getMaxDamage()) {
                    UtilItem.breakItem(player, itemStack);
                    return false;
                }
                damageable.setDamage(currentDamage + playerItemDamageEvent.getDamage());
            }
        }
        playerItemDamageEvent.getItem().setItemMeta(itemMeta);
        return false;
        /*BPvPItem item = itemHandler.getItem(itemStack);
        if (item != null && item.getMaxDurability() >= 0) {
            item.damageItem(player, itemStack, damage);
            return true;
        }
        return false;*/
    }


}
