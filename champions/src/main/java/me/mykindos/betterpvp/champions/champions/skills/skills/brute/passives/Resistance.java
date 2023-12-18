package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.configuration.type.fallback.FallbackValue;
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

@Singleton
@BPvPListener
public class Resistance extends Skill implements PassiveSkill {

    public int increasePerLevel;

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
                "You take <val>" + getPercent(level) + "%</val> less damage",
                "but you deal <val>" + getPercent(level) + "%</val> less damage as well"
        };
    }

    private int getPercent(int level) {
        return level * increasePerLevel;
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
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
                double modifier = getPercent(level);
                double modifier2 = modifier >= 10 ? 0.01 : 0.1;

                event.setDamage(event.getDamage() * (1.0 - (modifier * modifier2)));
            }

        }
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig(){
        increasePerLevel = getConfig("increasePerLevel", 15, Integer.class);
    }
}
