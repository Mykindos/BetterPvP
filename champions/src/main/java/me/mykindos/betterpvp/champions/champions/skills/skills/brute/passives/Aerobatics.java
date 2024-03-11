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
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

@Singleton
@BPvPListener
public class Aerobatics extends Skill implements PassiveSkill {

    private double percentIncreasePerLevel;
    private double percent;

    @Inject
    public Aerobatics(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Aerobatics";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "While in the air you deal <val>" + (int)(getPercent(level) * 100) + "%</val> more damage",
        };
    }

    private double getPercent(int level) {
        return percent + ((level - 1) * percentIncreasePerLevel);
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getDamager() instanceof Player damager) {
            int level = getLevel(damager);
            if (level > 0) {
                if(UtilBlock.airFoliage(damager.getLocation().add(0,-1, 0).getBlock())){
                    double modifier = getPercent(level);
                    event.setDamage(event.getDamage() * (1.0 + modifier));
                }
            }
        }
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig() {
        percent = getConfig("maxDamage", 0.3, Double.class);
        percentIncreasePerLevel = getConfig("maxDamageIncreasePerLevel", 0.1, Double.class);
    }
}
