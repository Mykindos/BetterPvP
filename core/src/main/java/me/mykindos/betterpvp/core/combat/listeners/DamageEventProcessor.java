package me.mykindos.betterpvp.core.combat.listeners;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.data.FireData;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseRegistry;
import me.mykindos.betterpvp.core.combat.delay.DamageDelayManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.cause.VanillaDamageCause;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;

import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.GameMode;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.WeakHashMap;

/**
 * Main entry point for processing vanilla damage events into our custom system
 */
@SuppressWarnings("ALL")
@CustomLog
@BPvPListener
public class DamageEventProcessor implements Listener {

    private final Core core;
    private final DamageCauseRegistry causeRegistry;
    private final DamageDelayManager delayManager;
    private final DamageEventFinalizer finalizer;
    private final WeakHashMap<LivingEntity, FireData> fireDamageSource = new WeakHashMap<>();
    
    @Inject
    private DamageEventProcessor(Core core, DamageCauseRegistry causeRegistry, DamageDelayManager delayManager, DamageEventFinalizer finalizer) {
        this.core = core;
        this.causeRegistry = causeRegistry;
        this.delayManager = delayManager;
        this.finalizer = finalizer;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        // Handle edge cases first
        if (shouldCancelDamage(event)) {
            event.setCancelled(true);
            return;
        }
        
        // Process damage source modifications
        DamageSource source = processDamageSource(event);
        
        // Get our custom damage cause
        VanillaDamageCause cause = causeRegistry.fromVanilla(event.getCause());
        
        // Create our custom damage event
        DamageEvent damageEvent = createDamageEvent(event, source, cause);

        // Cancel the vanilla event to prevent double processing
        event.setCancelled(true);

        // Process the damage event
        processDamageEvent(damageEvent);
    }

    public boolean processDamageEvent(DamageEvent damageEvent) {
        if (damageEvent.getDamagee().isInvulnerable()
                || (damageEvent.getDamagee() instanceof HumanEntity human && human.getGameMode().isInvulnerable())) {
            return false; // Entity is invulnerable
        }

        // Check if we can proceed with damage due to damage delays
        if (!delayManager.processDamageDelay(damageEvent)) return false;

        // Check if they can be hurt
        if (damageEvent.isDamageeLiving() && damageEvent.getDamager() instanceof LivingEntity livingDamager) {
            final EntityCanHurtEntityEvent event = new EntityCanHurtEntityEvent(livingDamager, damageEvent.getLivingDamagee());
            event.callEvent();
            if (!event.isAllowed()) {
                return false; // Entity cannot be hurt
            }
        }

        // Fire our custom event
        UtilServer.callEvent(damageEvent);

        if (damageEvent.isCancelled()) {
            return false;
        }

        // Call the finalizer
        finalizer.finalizeEvent(damageEvent);
        return true;
    }

    /**
     * Checks if damage should be cancelled based on various conditions
     */
    private boolean shouldCancelDamage(EntityDamageEvent event) {
        // Cancel lightning damage
        if (event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING) {
            return true;
        }
        
        // Cancel wither damage
        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            return true;
        }

        if (event instanceof EntityDamageByEntityEvent entityEvent) {
            if (entityEvent.getDamager() instanceof EvokerFangs) {
                // Cancel Evoker Fangs damage
                return true;
            }
            
            if (entityEvent.getDamager() instanceof FishHook fishHook) {
                // Allow fishing hook damage only from players
                return !(fishHook.getShooter() instanceof Player);
            }
        }
        
        // Cancel poison damage if entity would die
        if (event.getCause() == EntityDamageEvent.DamageCause.POISON) {
            if (event.getEntity() instanceof LivingEntity living && living.getHealth() < 2) {
                return true;
            }
        }
        
        // Cancel damage from entities with NO_DAMAGE tag
        if (event instanceof EntityDamageByEntityEvent entityEvent) {
            Boolean noDamage = entityEvent.getDamager().getPersistentDataContainer()
                    .get(CoreNamespaceKeys.NO_DAMAGE, PersistentDataType.BOOLEAN);
            if (noDamage != null && noDamage) {
                return true;
            }
        }
        
        // Cancel damage to creative/spectator players
        if (event.getEntity() instanceof Player player) {
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Processes and modifies the damage source as needed
     */
    private DamageSource processDamageSource(EntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        
        // Handle TNT damage source modification
        if (source.getDirectEntity() instanceof TNTPrimed tnt && tnt.getSource() != null) {
            source = DamageSource.builder(DamageType.PLAYER_EXPLOSION)
                    .withDirectEntity(tnt)
                    .withCausingEntity(tnt.getSource())
                    .withDamageLocation(source.getDirectEntity().getLocation())
                    .build();
        }
        
        // Handle fire tick damage source modification
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK && 
            event.getEntity() instanceof LivingEntity living) {
            
            if (fireDamageSource.containsKey(living)) {
                FireData fireData = fireDamageSource.get(living);
                source = DamageSource.builder(DamageType.ON_FIRE)
                        .withDirectEntity(fireData.getDamager())
                        .withCausingEntity(fireData.getDamager())
                        .build();
            }
        }
        
        return source;
    }
    
    /**
     * Creates our custom damage event from the vanilla event
     */
    private DamageEvent createDamageEvent(EntityDamageEvent event, DamageSource source, VanillaDamageCause cause) {
        LivingEntity damager = (LivingEntity) (source.isIndirect() ? source.getCausingEntity() : source.getDirectEntity());
        
        // Determine knockback setting
        boolean knockback = cause.allowsKnockback();
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            knockback = false;
        }
        
        // Create the damage event
        DamageEvent damageEvent = new DamageEvent(
            event.getEntity(),
            damager,
            source.getDirectEntity(),
            source,
            cause,
            event.getDamage()
        );
        
        damageEvent.setKnockback(knockback);
        
        return damageEvent;
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        if (!(event.getCombuster() instanceof LivingEntity combusterEntity)) return;
        
        this.fireDamageSource.put(livingEntity,
                new FireData(combusterEntity,
                        (long) (event.getDuration() * 20L * 1000L))
        );
        
        log.debug("Registered fire damage source: {} -> {}", 
                 combusterEntity.getName(), livingEntity.getName()).submit();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onEntityCanHurt(EntityCanHurtEntityEvent event) {
        if (event.getDamagee() instanceof HumanEntity human && human.getGameMode().isInvulnerable()) {
            event.setResult(Event.Result.DENY);
        }
    }

    @UpdateEvent
    void delayUpdater() {
        fireDamageSource.forEach((livingEntity, fireData) -> {
            if (!UtilTime.elapsed(fireData.getStart(), fireData.getDuration() + 50L)) {
                return;
            }

            UtilServer.runTaskLater(core, () -> fireDamageSource.remove(livingEntity), 1L);
        });
    }
}
