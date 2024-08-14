package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.items.ItemHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

@BPvPListener
@Singleton
public class ProtectionListener implements Listener {

    @Inject
    @Config(path = "protection.drop-pickup-time", defaultValue = "10.0")
    private double dropPickupTime;

    private final EffectManager effectManager;
    private final ItemHandler itemHandler;

    @Inject
    public ProtectionListener(EffectManager effectManager, ItemHandler itemHandler) {
        this.effectManager = effectManager;
        this.itemHandler = itemHandler;
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) return;
        if (event.getItem().getOwner() == player.getUniqueId()) return;
        if (event.getItem().getThrower() == player.getUniqueId()) return;
        UtilMessage.message(player, "Protection", "You cannot pick up this item with protection");
        EffectTypes.disableProtectionReminder(player);
        event.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockDropItemEvent event) {
        if (event.isCancelled()) return;
        if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;
        UtilMessage.message(event.getPlayer(), "Protection", "You have <yellow>%s</yellow> seconds to pick up the items");
        event.getItems().forEach(item -> {
            item.setOwner(event.getPlayer().getUniqueId());
            UtilServer.runTaskLaterAsync(JavaPlugin.getPlugin(Core.class), () ->
                    item.setOwner(null), 10 * 20L);
        });
    }

    @EventHandler
    public void entDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee && event.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damagee, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "This is a new player and has protection!");
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(damager, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "You cannot damage other players while you have protection!");
                EffectTypes.disableProtectionReminder(damager);
                event.setCancelled(true);
            }
        }
    }

}
