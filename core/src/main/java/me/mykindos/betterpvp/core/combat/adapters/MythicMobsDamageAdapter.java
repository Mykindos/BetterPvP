package me.mykindos.betterpvp.core.combat.adapters;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractPlayer;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.events.ServerStartEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Iterator;
import java.util.Optional;

@PluginAdapter("MythicMobs")
@Singleton
@BPvPListener
public class MythicMobsDamageAdapter implements Listener {

    private final Core core;

    @Inject
    public MythicMobsDamageAdapter(Core core) {
        this.core = core;
    }

    @EventHandler
    public void onServerStart(ServerStartEvent event) { // Cleanse fresh loaded worlds of any mythic mobs
        UtilServer.runTaskLater(core, () -> {
            Iterator<ActiveMob> iterator = MythicBukkit.inst().getMobManager().getMobRegistry().values().iterator();
            while (iterator.hasNext()) {
                ActiveMob am = iterator.next();
                am.setDespawned();
                MythicBukkit.inst().getMobManager().unregisterActiveMob(am);
                if (am.getEntity() != null) {
                    am.getEntity().remove();
                }
                iterator.remove();

            }

        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void processDamageModifiers(CustomDamageEvent event) {
        var mobManager = MythicBukkit.inst().getMobManager();
        ActiveMob damagee = mobManager.getActiveMob(event.getDamagee().getUniqueId()).orElse(null);
        if (damagee != null) {

            if(damagee.getType().getDigOutOfGround()) {
                if(event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION) {
                    Entity bukkitEntity = damagee.getEntity().getBukkitEntity();
                    bukkitEntity.teleport(bukkitEntity.getLocation().add(0, 2, 0));
                }
            }

            if (damagee.getType().getDamageModifiers().containsKey(event.getCause().name())) {
                event.setDamage(event.getDamage() * damagee.getType().getDamageModifiers().get(event.getCause().name()));
            }

            if (event.getDamager() instanceof Player player) {
                if (damagee.getType().usesThreatTable()) {

                    AbstractEntity topThreatHolder = damagee.getThreatTable().getTopThreatHolder();
                    if(topThreatHolder != null && !topThreatHolder.getBukkitEntity().getUniqueId().equals(player.getUniqueId())) {
                        damagee.getThreatTable().decayTargetThreat();
                    }

                    AbstractPlayer mmPlayer = BukkitAdapter.adapt(player);
                    damagee.getThreatTable().threatGain(mmPlayer, event.getDamage());


                }
            }
        }
    }


    @EventHandler
    public void onFetchEntity(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(entity -> UtilFormat.stripColor(entity.getKey().getName()).isEmpty());
    }

    @EventHandler
    public void onTrainingDummyVelocity(CustomEntityVelocityEvent event) {
        if (isFixedTrainingDummy(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTrainingDummyKnockback(CustomKnockbackEvent event) {
        if (isFixedTrainingDummy(event.getDamagee())) {
            event.setCancelled(true);
        }
    }

    private boolean isFixedTrainingDummy(Entity entity) {
        MobExecutor mobManager = MythicBukkit.inst().getMobManager();
        final Optional<ActiveMob> mobOpt = mobManager.getActiveMob(entity.getUniqueId());
        if (mobOpt.isEmpty()) {
            return false;
        }

        final ActiveMob activeMob = mobOpt.get();
        return activeMob.getType().getInternalName().equals("Training_Dummy_Fixed");
    }



}
