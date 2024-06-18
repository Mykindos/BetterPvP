package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.OffensiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Precision extends Skill implements PassiveSkill, DamageSkill, OffensiveSkill {

    private double baseDamage;

    private double damageIncreasePerLevel;

    @Inject
    public Precision(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Precision";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Your arrows deal " + getValueString(this::getDamage, level) + " bonus damage on hit"
        };
    }

    public double getDamage(int level) {
        return baseDamage + (damageIncreasePerLevel * (level - 1));
    }

    @Override
    public Role getClassType() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getProjectile() instanceof Arrow)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if (level > 0) {
            event.setDamage(event.getDamage() + getDamage(level));
        }

    }

    @Override
    public void loadSkillConfig() {
        baseDamage = getConfig("baseDamage", 0.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 0.5, Double.class);
    }
}
