package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
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

@Getter
@Singleton
@BPvPListener
public class Precision extends Skill implements PassiveSkill, DamageSkill, OffensiveSkill {

    private double damage;


    @Inject
    public Precision(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Precision";
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "Your arrows deal <val>" + getDamage() + "</val> bonus damage on hit"
        };
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

        if (hasSkill(damager)) {
            event.setDamage(event.getDamage() + getDamage());
        }

    }

    @Override
    public void loadSkillConfig() {
        damage = getConfig("damage", 0.5, Double.class);
    }
}
