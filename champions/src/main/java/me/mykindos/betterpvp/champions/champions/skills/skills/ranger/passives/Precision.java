package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Set;

@Singleton
@BPvPListener
public class Precision extends Skill implements PassiveSkill {

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

        return new String[] {
                "Your arrows deal <val>" + (level * 0.5) + "</val> bonus damage on hit"
        };
    }

    @Override
    public Set<Role> getClassTypes() {
        return Role.RANGER;
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSlow(CustomDamageEvent event) {
        if(!(event.getProjectile() instanceof Arrow)) return;
        if(!(event.getDamager() instanceof Player damager)) return;

        int level = getLevel(damager);
        if(level > 0) {
            event.setDamage(event.getDamage() + (level * 0.5));
        }

    }

}
