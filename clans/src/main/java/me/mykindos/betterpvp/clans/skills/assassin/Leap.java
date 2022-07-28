package me.mykindos.betterpvp.clans.skills.assassin;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.skills.types.ClassType;
import me.mykindos.betterpvp.clans.skills.ISkill;
import me.mykindos.betterpvp.clans.skills.types.InteractSkill;
import me.mykindos.betterpvp.clans.skills.types.SkillActions;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

@Singleton
public class Leap implements ISkill, InteractSkill {

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
}
