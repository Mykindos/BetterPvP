package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.combat.damage.SkillDamageModifier;
import me.mykindos.betterpvp.core.combat.cause.DamageCauseCategory;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class Overwhelm extends Skill implements PassiveSkill, DamageSkill {

    private double bonusDamage;

    private double healthOverTarget;

    private double baseMaxDamage;

    private double maxDamageIncreasePerLevel;

    @Inject
    public Overwhelm(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Overwhelm";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You deal " + getValueString(this::getBonusDamage, level) + " bonus damage for every",
                getValueString(this::getHealthOverTarget, level) + " more health you have than your target",
                "",
                "You can deal a maximum of " + getValueString(this::getMaxDamage, level) + " bonus damage"
        };
    }

    public double getMaxDamage(int level) {
        return baseMaxDamage + ((level-1) * maxDamageIncreasePerLevel);
    }

    public double getBonusDamage(int level) {
        return bonusDamage;
    }

    public double getHealthOverTarget(int level) {
        return healthOverTarget;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(DamageEvent event) {
        if (!event.isDamageeLiving()) return;
        if (!event.getCause().getCategories().contains(DamageCauseCategory.MELEE)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        int level = getLevel(player);
        if (level > 0) {
            LivingEntity ent = event.getLivingDamagee();
            double difference = (player.getHealth() - ent.getHealth()) / healthOverTarget;
            if (difference > 0) {
                // Calculate damage first, THEN cap it
                double damageToAdd = difference * bonusDamage;
                damageToAdd = Math.min(damageToAdd, getMaxDamage(level));

                // Add a flat damage modifier based on health difference
                event.addModifier(new SkillDamageModifier.Flat(this, damageToAdd));
            }
        }
    }

    @Override
    public void loadSkillConfig(){
        bonusDamage = getConfig("bonusDamage", 0.5, Double.class);
        healthOverTarget = getConfig("healthOverTarget", 2.0, Double.class);
        baseMaxDamage = getConfig("baseMaxDamage", 1.0, Double.class);
        maxDamageIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 0.5, Double.class);
    }


}
