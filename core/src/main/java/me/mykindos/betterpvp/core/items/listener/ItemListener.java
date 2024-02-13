package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.events.CustomDamageDurabilityEvent;
import me.mykindos.betterpvp.core.items.BPvPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageItem(PlayerItemDamageEvent event) {
        if (event.isCancelled()) return;
        event.setCancelled(damageCustomItem(event.getPlayer(), event.getItem(), 1));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCustomDamageDurability(CustomDamageDurabilityEvent event) {
        if (event.isDamagerTakeDurability() && event.getCustomDamageEvent().getDamager() instanceof Player damager) {
            if (event.getCustomDamageEvent().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                if (damageCustomItem(damager, damager.getInventory().getItemInMainHand(), 1)) {
                    //durability was handled, cancel it
                    event.setDamagerTakeDurability(false);
                }
            }
        }
        if (event.isDamageeTakeDurability() && event.getCustomDamageEvent().getDamagee() instanceof Player damagee) {
            for (ItemStack armour : damagee.getEquipment().getArmorContents()) {
                if (armour == null) continue;
                if (damageCustomItem(damagee, armour, 1)) {
                    //durability was handled at least once, so cancel it (not perfect)
                    event.setDamageeTakeDurability(false);
                }
            }
        }
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
