package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.events.CustomDamageDurabilityEvent;
import me.mykindos.betterpvp.core.items.BPVPItem;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamageItem(PlayerItemDamageEvent event) {
        log.info("itemdamaged");
        log.info(event.getItem().toString());
        if (event.isCancelled()) return;
        event.setCancelled(damageCustomItem(event.getPlayer(), event.getItem(), event.getOriginalDamage()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCustomDamageDurability(CustomDamageDurabilityEvent event) {
        log.info("itemcustomdamaged");
        if (event.isDamagerTakeDurability() && event.getCustomDamageEvent().getDamager() instanceof Player damager) {
            if (damageCustomItem(damager, damager.getInventory().getItemInMainHand(), 1)) {
                //durability was handled, cancel it
                event.setDamagerTakeDurability(false);
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

    public boolean damageCustomItem(Player player, ItemStack itemStack, int damage) {
        BPVPItem item = itemHandler.getItem(itemStack);
        if (item != null && item.getMaxDurability() >= 0) {
            item.damageItem(player, itemStack, damage);
            return true;
        }
        return false;
    }


}
