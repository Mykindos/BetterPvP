package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillMetadataImpl;
import io.lumine.mythic.core.skills.SkillTriggers;
import me.mykindos.betterpvp.core.combat.adapters.CustomDamageAdapter;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;

public class MythicMobsAdapter implements CustomDamageAdapter {

    @Override
    public boolean processCustomDamageAdapter(CustomDamageEvent event, double damage) {
        ActiveMob mythicMob = MythicBukkit.inst().getMobManager().getActiveMob(event.getDamagee().getUniqueId()).orElse(null);
        if (mythicMob != null) {

            if (event.getDamager() == null) {
                return false;
            }

            var skillMetaData = new SkillMetadataImpl(SkillTriggers.DAMAGED, mythicMob, new BukkitEntity(event.getDamager()));
            skillMetaData.getVariables().putString("damage-cause", event.getCause().toString());
            skillMetaData.getVariables().putString("damage-amount", String.valueOf(damage));
            skillMetaData.getVariables().putObject("damage-metadata", skillMetaData);
            skillMetaData.getVariables().putString("real-damage-cause", event.getCause().toString());
            mythicMob.getType().executeSkills(skillMetaData.getCause(), skillMetaData);
            return true;
        }

        return false;
    }
}
