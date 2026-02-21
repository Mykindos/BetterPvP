package me.mykindos.betterpvp.champions.champions.builds.menus.events;

import me.mykindos.betterpvp.champions.champions.builds.BuildSkill;
import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import org.bukkit.entity.Player;


public class SkillEquipEvent extends SkillUpdateEvent {


    public SkillEquipEvent(Player player, BuildSkill skill, RoleBuild roleBuild, RoleBuild previous) {
        super(player, skill, roleBuild, previous);
    }
}
