package me.mykindos.betterpvp.core.effects.listeners.effects;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@BPvPListener
@Singleton
public class FrozenListener implements Listener {

    private final EffectManager effectManager;

    @Inject
    public FrozenListener(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.hasChangedPosition()) {
            if (effectManager.hasEffect(event.getPlayer(), EffectTypes.FROZEN)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled()) return;
        if (effectManager.hasEffect(event.getPlayer(), EffectTypes.FROZEN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickupItem(PlayerAttemptPickupItemEvent event) {
        if (event.isCancelled()) return;
        if (effectManager.hasEffect(event.getPlayer(), EffectTypes.FROZEN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEvent(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY
                && event.useItemInHand() == Event.Result.DENY) {
            //Both events are denied, this is a cancelled event
            return;
        }
        if (effectManager.hasEffect(event.getPlayer(), EffectTypes.FROZEN)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
            UtilMessage.message(event.getPlayer(), "core.prefix.frozen", "core.effect.frozen.cannot_interact");
        }
    }

    @EventHandler
    public void entityDamageEvent(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (effectManager.hasEffect(player, EffectTypes.FROZEN)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void entDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof LivingEntity damagee) {
            if (effectManager.hasEffect(damagee, EffectTypes.FROZEN)) {
                UtilMessage.message(event.getDamager(), "core.prefix.frozen", "core.effect.frozen.target_immune",
                        Component.text(damagee.getName(), NamedTextColor.YELLOW));
                event.setCancelled(true);
            }
        }

        if (event.getDamager() instanceof LivingEntity damager) {
            if (effectManager.hasEffect(damager, EffectTypes.FROZEN)) {
                UtilMessage.message(damager, "core.prefix.frozen", "core.effect.frozen.cannot_damage");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if (event.getDamagee() instanceof LivingEntity damagee) {
            if (effectManager.hasEffect(damagee, EffectTypes.FROZEN)) {
                UtilMessage.message(event.getDamager(), "core.prefix.frozen", "core.effect.frozen.target_immune",
                        Component.text(damagee.getName(), NamedTextColor.YELLOW));
                event.setResult(Event.Result.DENY);
            }
        }

        if (event.getDamager() instanceof LivingEntity damager) {
            if (effectManager.hasEffect(damager, EffectTypes.FROZEN)) {
                UtilMessage.message(damager, "core.prefix.frozen", "core.effect.frozen.cannot_damage");
                event.setResult(Event.Result.DENY);
            }
        }
    }




}
