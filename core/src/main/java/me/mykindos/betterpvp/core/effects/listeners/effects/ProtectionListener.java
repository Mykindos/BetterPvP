package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.events.ThrowableHitEntityEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

@BPvPListener
@Singleton
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
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) return;
        if (event.getItem().getThrower() == player.getUniqueId()) return;

        if (event.getItem().getOwner() == null || !event.getItem().getOwner().equals(player.getUniqueId())) {
            if (cooldownManager.use(player, "protectionitempickup", 5.0, false)) {
                UtilMessage.message(player, "Protection", "You cannot pick up this item with protection");
                EffectTypes.disableProtectionReminder(player);
            }
            event.setCancelled(true);
        }

    }

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
        if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;
        UtilItem.reserveItem(event.getItemDrop(), event.getPlayer(), 10);
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockDropItemEvent event) {
        if (event.isCancelled()) return;
        if (!effectManager.hasEffect(event.getPlayer(), EffectTypes.PROTECTION)) return;
        UtilMessage.message(event.getPlayer(), "Protection", "You have <yellow>%s</yellow> seconds to pick up the items", UtilFormat.formatNumber(dropPickupTime));
        event.getItems().forEach(item -> {
            UtilItem.reserveItem(item, event.getPlayer(), 10);
        });
    }

    @EventHandler
    public void entDamage(PreCustomDamageEvent event) {
        if (event.getCustomDamageEvent().getDamagee() instanceof Player damagee &&
                event.getCustomDamageEvent().getDamager() instanceof Player damager) {
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

            if (target.equals(source)) return;

            if (effectManager.hasEffect(target, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }

            if (effectManager.hasEffect(source, EffectTypes.PROTECTION)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler(ignoreCancelled = true)
    public void onEffectReceive(EffectReceiveEvent event) {
        if (!(event.getEffect().getApplier() instanceof final Player applier)) return;
        if (!(event.getTarget() instanceof final Player target)) return;
        //allow self effects
        if (applier.equals(target)) return;
        //prevent all giving other effects
        if (effectManager.hasEffect(applier, EffectTypes.PROTECTION)) {
            event.setCancelled(true);
        }
        //prevent all receiving other effects
        if (effectManager.hasEffect(target, EffectTypes.PROTECTION)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(ignoreCancelled = true)
    public void onFishingRodHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof FishingHook fishingHook)) return;
        if (!(event.getHitEntity() instanceof Player target)) return;
        if (!((fishingHook.getOwner()) instanceof LivingEntity caster)) return;
        if ((effectManager.hasEffect(target, EffectTypes.PROTECTION))) {
            event.setCancelled(true);
        }
        if (effectManager.hasEffect(caster, EffectTypes.PROTECTION)) {
            event.setCancelled(true);
        }

    }

}
