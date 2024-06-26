package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.UtilitySkill;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.VelocityType;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;


@Singleton
@BPvPListener
public class Colossus extends Skill implements PassiveSkill, UtilitySkill {

    private double reductionPerLevel;

    @Inject
    public Colossus(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Colossus";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "You take <val>" + ((reductionPerLevel * 100) * level) + "</val>% reduced knockback"
        };
    }

    @Override
    public Role getClassType() {
        return Role.BRUTE;
    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @EventHandler
    public void onCustomVelocity(CustomEntityVelocityEvent event) {
        if(!(event.getEntity() instanceof Player player)) return;
        if(event.getVelocityType() != VelocityType.KNOCKBACK && event.getVelocityType() != VelocityType.KNOCKBACK_CUSTOM) return;

        int level = getLevel(player);
        if(level > 0) {
            event.setVector(event.getVector().multiply(1 - ((reductionPerLevel) * level)));
        }
    }

    @Override
    public void loadSkillConfig(){
        reductionPerLevel = getConfig("reductionPerLevel", 0.20, Double.class);
    }

}
