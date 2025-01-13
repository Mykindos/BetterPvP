package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Getter
@Singleton
@BPvPListener
public class Cleave extends Skill implements PassiveSkill, Listener, OffensiveSkill, CrowdControlSkill {

    private double distance;
    private double percentageOfDamage;
    private int maxEnemiesHit;

    @Inject
    public Cleave(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cleave";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Your axe attacks deal <val>" + UtilFormat.formatNumber(getPercentageOfDamage() * 100, 0) + "</val> of your damage to",
                "all enemies within <val>" + getDistance() + "</val> blocks of your target enemy.",
                "",
                "Max Enemies Hit: <val>" + getMaxEnemiesHit()
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler
    public void onCustomDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!SkillWeapons.isHolding(damager, SkillType.AXE)) return;
        if (event.hasReason(getName())) return; // Don't get stuck in an endless damage loop
        if (event.getDamagee().hasMetadata("PlayerSpawned")) return;

        int enemiesHit = 0;
        if (hasSkill(damager)) {
            event.getDamagee().getWorld().spawnParticle(Particle.SWEEP_ATTACK, event.getDamagee().getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            for (var target : UtilEntity.getNearbyEntities(damager, event.getDamagee().getLocation(), getDistance(), EntityProperty.ENEMY)) {
                if (target.get().equals(event.getDamagee())) continue;
                if (!damager.hasLineOfSight(target.getKey())) continue;
                if (enemiesHit >= getMaxEnemiesHit()) continue;

                CustomDamageEvent cde = new CustomDamageEvent(target.getKey(), damager, null, DamageCause.ENTITY_ATTACK, event.getDamage() * getPercentageOfDamage(), true, getName());
                cde.setDoDurability(false);
                UtilDamage.doCustomDamage(cde);
                enemiesHit++;
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        distance = getConfig("distance", 3.0, Double.class);
        percentageOfDamage = getConfig("percentageOfDamage", 0.5, Double.class);
        maxEnemiesHit = getConfig("maxEnemiesHit", 5, Integer.class);
    }
}
