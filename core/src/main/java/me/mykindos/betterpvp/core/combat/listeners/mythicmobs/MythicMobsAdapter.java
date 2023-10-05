package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillMetadataImpl;
import io.lumine.mythic.core.skills.SkillTriggers;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class MythicMobsAdapter implements CustomDamageAdapter {

    @Override
    public boolean processPreCustomDamage(CustomDamageEvent event) {
        if(event.getDamager() == null) return true;
        ActiveMob damagerMythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamager().getUniqueId()).orElse(null);
        if (damagerMythicMob != null) {

            if(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                var damagedMetaData = new SkillMetadataImpl(SkillTriggers.ATTACK, damagerMythicMob, new BukkitEntity(event.getDamagee()));
                setMetaData(event, event.getDamage(), damagerMythicMob, damagedMetaData);
            }

            if(damagerMythicMob.getEntity().getMetadata("doing-skill-damage").isEmpty()) {
                event.setCancelled(true);
                return false;
            }

            // Allow to return false so damage sounds can play
        }

        return true;
    }

    @Override
    public boolean processCustomDamageAdapter(CustomDamageEvent event, double damage) {
        if (event.getDamager() == null) {
            return false;
        }

        ActiveMob damagedMythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamagee().getUniqueId()).orElse(null);
        if (damagedMythicMob != null) {

            var damagedMetaData = new SkillMetadataImpl(SkillTriggers.DAMAGED, damagedMythicMob, new BukkitEntity(event.getDamager()));
            setMetaData(event, damage, damagedMythicMob, damagedMetaData);
            return true;
        }

        ActiveMob damagerMythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamager().getUniqueId()).orElse(null);
        if (damagerMythicMob != null) {

            var damagedMetaData = new SkillMetadataImpl(SkillTriggers.ATTACK, damagerMythicMob, new BukkitEntity(event.getDamagee()));
            setMetaData(event, damage, damagerMythicMob, damagedMetaData);

            // Allow to return false so damage sounds can play
        }


        return false;
    }

    private void setMetaData(CustomDamageEvent event, double damage, ActiveMob damagerMythicMob, SkillMetadataImpl damagedMetaData) {
        damagedMetaData.getVariables().putString("damage-cause", event.getCause().toString());
        damagedMetaData.getVariables().putString("damage-amount", String.valueOf(damage));
        damagedMetaData.getVariables().putObject("damage-metadata", damagedMetaData);
        damagedMetaData.getVariables().putString("real-damage-cause", event.getCause().toString());
        damagerMythicMob.getType().executeSkills(damagedMetaData.getCause(), damagedMetaData);
    }

    @Override
    public void processKnockbackAdapter(CustomDamageEvent event, boolean knockback) {
        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamagee().getUniqueId()).orElse(null);
        if (mythicMob != null) {
            if (event.getDamager() == null) {
                return;
            }

            event.setKnockback(knockback);
        }
    }
}
