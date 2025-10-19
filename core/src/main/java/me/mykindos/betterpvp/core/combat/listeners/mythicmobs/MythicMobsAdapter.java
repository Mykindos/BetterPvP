package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ModeledEntity;
import io.lumine.mythic.api.skills.damage.DamageMetadata;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillMetadataImpl;
import io.lumine.mythic.core.skills.SkillTriggers;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.events.CustomKnockbackEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.listeners.DamageEventProcessor;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@PluginAdapter("MythicMobs")
@BPvPListener
@Singleton
public class MythicMobsAdapter implements CustomDamageAdapter, Listener {

    @Inject
    private MythicMobsAdapter(DamageEventProcessor damageEventProcessor) {
        damageEventProcessor.registerCustomDamageAdapter(this);
    }

    @EventHandler
    void onDamageKnockback(CustomKnockbackEvent event) {
        var mobManager = MythicBukkit.inst().getMobManager();

        final Optional<ActiveMob> mobOptional = mobManager.getActiveMob(event.getDamagee().getUniqueId());
        if (mobOptional.isEmpty()) {
            return;
        }

        // Clamp the resistance between 0 and 1
        final ActiveMob mob = mobOptional.get();
        final double knockbackResistance = Math.max(0, Math.min(1, mob.getType().getKnockbackResistance(mob)));
        final double multiplier = 1 - knockbackResistance;
        event.setMultiplier(multiplier);
    }

    @Override
    public boolean isValid(DamageEvent event) {
        var mobManager = MythicBukkit.inst().getMobManager();

        if (event.getDamager() != null && mobManager.getActiveMob(event.getDamager().getUniqueId()).isPresent()) {
            return true;
        }

        return mobManager.getActiveMob(event.getDamagee().getUniqueId()).isPresent();
    }

    @Override
    public boolean processPreCustomDamage(DamageEvent event) {
        if (event.getDamager() == null) return true;
        var mobManager = MythicBukkit.inst().getMobManager();
        ActiveMob damagerMythicMob = mobManager.getActiveMob(event.getDamager().getUniqueId()).orElse(null);
        if (damagerMythicMob != null) {

            if (event.getBukkitCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {

                var damagedMetaData = new SkillMetadataImpl(SkillTriggers.ATTACK, damagerMythicMob, new BukkitEntity(event.getDamagee()));
                setMetaData(event, event.getDamage(), damagerMythicMob, damagedMetaData);
            }

            if (damagerMythicMob.getEntity().getMetadata("doing-skill-damage").isEmpty() && mobManager.getActiveMob(event.getDamagee().getUniqueId()).isEmpty()) {
                event.setCancelled(true);
                return false;
            }

        }

        applyDamageImmunityOverride(event);

        return true;
    }

    @Override
    public boolean processCustomDamageAdapter(DamageEvent event) {
        if (event.getDamager() == null) {
            return false;
        }

        ActiveMob damagedMythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamagee().getUniqueId()).orElse(null);
        if (damagedMythicMob != null) {
            ModeledEntity modeledEntity = ModelEngineAPI.getModeledEntity(event.getDamagee());
            if (modeledEntity != null) {
                modeledEntity.markHurt();
            }
            var damagedMetaData = new SkillMetadataImpl(SkillTriggers.DAMAGED, damagedMythicMob, new BukkitEntity(event.getDamager()));
            setMetaData(event, event.getDamage(), damagedMythicMob, damagedMetaData);

            if (event.getDamager() instanceof Player player) {
                player.playSound(event.getDamagee().getLocation(), Sound.ENTITY_PLAYER_HURT, 0.6f, 0.8f);
            }

            return true;
        }

        ActiveMob damagerMythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamager().getUniqueId()).orElse(null);
        if (damagerMythicMob != null) {

            var damagedMetaData = new SkillMetadataImpl(SkillTriggers.ATTACK, damagerMythicMob, new BukkitEntity(event.getDamagee()));
            setMetaData(event, event.getDamage(), damagerMythicMob, damagedMetaData);

            // Allow to return false so damage sounds can play
        }

        applyDamageImmunityOverride(event);

        return false;
    }

    private void setMetaData(DamageEvent event, double damage, ActiveMob damagerMythicMob, SkillMetadataImpl damagedMetaData) {
        damagedMetaData.getVariables().putString("damage-cause", event.getCause().toString());
        damagedMetaData.getVariables().putString("damage-amount", String.valueOf(damage));
        damagedMetaData.getVariables().putObject("damage-metadata", damagedMetaData);
        damagedMetaData.getVariables().putString("real-damage-cause", event.getCause().toString());
        damagerMythicMob.getType().executeSkills(damagedMetaData.getCause(), damagedMetaData);
    }

    private void applyDamageImmunityOverride(DamageEvent event) {
        List<MetadataValue> metadata = event.getDamagee().getMetadata("skill-damage");
        if (event.isDamageeLiving() && !metadata.isEmpty() && metadata.getFirst().value() instanceof DamageMetadata dm) {
            if (Boolean.TRUE.equals(dm.getPreventsImmunity())) {
                event.setDamageDelay(0);
                event.setForceDamageDelay(0);
                Objects.requireNonNull(event.getLivingDamagee()).setNoDamageTicks(0);
            }
        }
    }

}
