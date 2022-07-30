package me.mykindos.betterpvp.clans.skills.skills.assassin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.skills.Skill;
import me.mykindos.betterpvp.clans.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.skills.types.*;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
@WithReflection
public class Leap extends Skill implements InteractSkill, CooldownSkill, EnergySkill {

    @Inject
    public Leap(SkillConfigFactory configFactory) {
        super(configFactory);
    }

    @Override
    public String getName() {
        return "Leap";
    }

    @Override
    public String getDescription(int level) {
        return String.format("""
                This is a demo description
                
                Level: %d
                
                Right click to activate.
                """, level);
    }

    @Override
    public ClassType getClassType() {
        return ClassType.ASSASSIN;
    }

    @Override
    public void activate(Player player) {
        player.sendMessage("You used leap");
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public int getCooldown(int level) {
        return getSkillConfig().getCooldown();
    }

    @Override
    public int getEnergy(int level) {
        return getSkillConfig().getEnergyCost();
    }
}
