package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Set;

@Singleton
@BPvPListener
public class Resistance extends Skill implements PassiveSkill {

    @Inject
    public Resistance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Resistance";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You take <val>" + (level * 15) + "%</val> less damage",
                "but you deal <val>" + (level * 15) + "%</val> less damage as well"
        };
    }
    @Override
    public String getDefaultClassString() {
        return "brute";
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamagee() instanceof Player damagee) {
            int level = getLevel(damagee);
            if (level > 0) {
                double modifier = level * 15;
                double modifier2 = modifier >= 10 ? 0.01 : 0.1;

                event.setDamage(event.getDamage() * (1.0 - (modifier * modifier2)));
            }
        }


        if (event.getDamager() instanceof Player damager) {
            int level = getLevel(damager);
            if(level > 0) {
                double modifier = level * 15;
                double modifier2 = modifier >= 10 ? 0.01 : 0.1;

                event.setDamage(event.getDamage() * (1.0 - (modifier * modifier2)));
            }

        }
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

}
