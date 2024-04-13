package me.mykindos.betterpvp.champions.champions.skills.skills.global;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.energy.events.RegenerateEnergyEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

@Singleton
@BPvPListener
public class FastRecovery extends Skill implements PassiveSkill {

    private double basePercentage;
    private double percentagePerLevel;

    @Inject
    public FastRecovery(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Fast Recovery";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Increase your energy regeneration speed",
                "by <val>" + getPercentage(level) * 100 + "%",
                "",
                "Does not work with legendary items equipped"

        };
    }

    public double getPercentage(int level) {
        return basePercentage + ((level - 1) * percentagePerLevel);
    }

    @Override
    public Role getClassType() {
        return null;
    }

    @EventHandler
    public void onEnergyRegen(RegenerateEnergyEvent event) {
        Player player = event.getPlayer();

        int level = getLevel(player);
        if(level > 0) {
            event.setEnergy(event.getEnergy() * (1.0 + getPercentage(level)));
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.GLOBAL;
    }

    @Override
    public void loadSkillConfig(){
        basePercentage = getConfig("basePercentage", 0.20, Double.class);
        percentagePerLevel = getConfig("percentagePerLevel", 0.20, Double.class);
    }

}
