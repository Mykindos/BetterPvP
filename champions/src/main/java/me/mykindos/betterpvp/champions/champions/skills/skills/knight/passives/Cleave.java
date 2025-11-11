package me.mykindos.betterpvp.champions.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CrowdControlSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageCause;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory.MELEE;

@Singleton
@BPvPListener
public class Cleave extends Skill implements PassiveSkill, Listener, OffensiveSkill, CrowdControlSkill {

    private double baseDistance;
    private double distanceIncreasePerLevel;
    private double percentageOfDamage;
    private double percentageOfDamageIncreasePerLevel;
    private int maxEnemiesHit;
    private int maxEnemiesHitIncreasePerLevel;

    @Inject
    public Cleave(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Cleave";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your axe attacks deal " + getValueString(this::getPercentageOfDamage, level, 100, "%", 0) + " of your damage to",
                "all enemies within " + getValueString(this::getDistance, level) + " blocks of your target enemy.",
                "",
                "Max Enemies Hit: " + getValueString(this::getMaxEnemiesHit, level, 0),
        };
    }

    public double getPercentageOfDamage(int level) {
        return percentageOfDamage + ((level - 1) * percentageOfDamageIncreasePerLevel);
    }

    public int getMaxEnemiesHit(int level) {
        return maxEnemiesHit + ((level - 1) * maxEnemiesHitIncreasePerLevel);
    }

    public double getDistance(int level) {
        return baseDistance + ((level - 1) * distanceIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onCustomDamage(DamageEvent event) {
        if (event.isCancelled()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (event.getCause() instanceof SkillDamageCause) return; // no skills triggering cleave
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!SkillWeapons.isHolding(damager, SkillType.AXE)) return;
        if (event.hasReason(getName())) return; // Don't get stuck in an endless damage loop
        if (event.getDamagee().hasMetadata("PlayerSpawned")) return;

        int level = getLevel(damager);

        int enemiesHit = 0;
        if (level > 0) {
            event.getDamagee().getWorld().spawnParticle(Particle.SWEEP_ATTACK, event.getDamagee().getLocation().add(0, 0.5, 0), 1, 0, 0, 0, 0);
            for (var target : UtilEntity.getNearbyEntities(damager, event.getDamagee().getLocation(), getDistance(level), EntityProperty.ENEMY)) {
                if (target.get().equals(event.getDamagee())) continue;
                if (!damager.hasLineOfSight(target.getKey())) continue;
                if (enemiesHit >= getMaxEnemiesHit(level)) continue;

                DamageEvent cde = new DamageEvent(target.getKey(),
                        damager,
                        null,
                        new SkillDamageCause(this).withCategory(MELEE).withKnockback(true),
                        event.getDamage() * getPercentageOfDamage(level),
                        getName());
                cde.getDurabilityParameters().disableAttackerDurability();
                UtilDamage.doDamage(cde);
                enemiesHit++;
            }
        }
    }

    @Override
    public void loadSkillConfig() {
        baseDistance = getConfig("baseDistance", 3.0, Double.class);
        distanceIncreasePerLevel = getConfig("distanceIncreasePerLevel", 0.0, Double.class);
        percentageOfDamage = getConfig("percentageOfDamage", 0.5, Double.class);
        percentageOfDamageIncreasePerLevel = getConfig("percentageOfDamageIncreasePerLevel", 0.25, Double.class);
        maxEnemiesHit = getConfig("maxEnemiesHit", 5, Integer.class);
        maxEnemiesHitIncreasePerLevel = getConfig("maxEnemiesHitIncreasePerLevel", 0, Integer.class);
    }
}
