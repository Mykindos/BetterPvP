package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

@BPvPListener
@Singleton
@Slf4j
public class ProtectionListener implements Listener {

    @Inject
    @Config(path = "protection.drop-pickup-time", defaultValue = "10.0")
    private double dropPickupTime;

    private final EffectManager effectManager;
    private final CooldownManager cooldownManager;

    @Inject
    public ProtectionListener(EffectManager effectManager, CooldownManager cooldownManager) {
        this.effectManager = effectManager;
        this.cooldownManager = cooldownManager;
    }

    // TODO reimplement
    //@EventHandler
    //public void onItemPickup(EntityPickupItemEvent event) {
    //    if (!(event.getEntity() instanceof Player player)) return;
    //    if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) return;
    //    if (event.getItem().getThrower() == player.getUniqueId()) return;
//
    //    if (event.getItem().getOwner() != null && !event.getItem().getOwner().equals(player.getUniqueId())) {
    //        if (cooldownManager.use(player, "protectionitempickup", 5.0, false)) {
    //            UtilMessage.message(player, "Protection", "You cannot pick up this item with protection");
    //            EffectTypes.disableProtectionReminder(player);
    //        }
//
//
    //        event.setCancelled(true);
    //    }
    //}

    @EventHandler
    public void onFetchNearbyEntity(FetchNearbyEntityEvent<?> event) {
        if (!(event.getSource() instanceof Player player)) return;

        event.getEntities().removeIf(ent -> {
            if(ent instanceof Player target) {
                if (effectManager.hasEffect(target, EffectTypes.PROTECTION)) {
                    return true;
                }
            }
            return false;
        });
    }


    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        //if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;
        //event.getItemDrop().setOwner(event.getPlayer().getUniqueId());
    }


    //@EventHandler(priority = EventPriority.HIGHEST)
    //public void onBlockBreak(BlockDropItemEvent event) {
    //    if (event.isCancelled()) return;
    //    if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;
    //    UtilMessage.message(event.getPlayer(), "Protection", "You have <yellow>%d</yellow> seconds to pick up the items", dropPickupTime);
    //    event.getItems().forEach(item -> {
    //        item.setOwner(event.getPlayer().getUniqueId());
    //        UtilServer.runTaskLaterAsync(JavaPlugin.getPlugin(Core.class), () ->
    //                item.setOwner(null), 10 * 20L);
    //    });
    //}

    @EventHandler
    public void entDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee && event.getDamager() instanceof Player damager) {
            if (effectManager.hasEffect(damagee, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "This is a new player and is protected from damage!");
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(damager, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "You cannot damage other players while you have protection!");
                EffectTypes.disableProtectionReminder(damager);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onThrowableHit(ThrowableHitEntityEvent event) {
        if (event.getCollision() instanceof Player damagee && event.getThrowable().getThrower() instanceof Player damager) {
            if (effectManager.hasEffect(damagee, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "This is a new player and is protected from damage!");
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(damager, EffectTypes.PROTECTION)) {
                UtilMessage.message(damager, "Protected", "You cannot damage other players while you have protection!");
                EffectTypes.disableProtectionReminder(damager);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCustomVelocity(CustomEntityVelocityEvent event) {
        if (event.getEntity() instanceof Player target && event.getSource() instanceof Player source) {

            if(target.equals(source)) return;

            if (effectManager.hasEffect(target, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(source, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onSetFire(EntityCombustByEntityEvent event) {
        if (event.getCombuster() instanceof Player damager && event.getEntity() instanceof Player damagee) {
            if (effectManager.hasEffect(damager, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(damagee, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }
        }
    }

}
