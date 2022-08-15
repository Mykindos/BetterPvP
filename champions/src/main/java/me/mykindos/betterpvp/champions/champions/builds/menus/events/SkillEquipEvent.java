package me.mykindos.betterpvp.champions.champions.builds.menus.events;

import me.mykindos.betterpvp.champions.champions.builds.RoleBuild;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import org.bukkit.entity.Player;


public class SkillEquipEvent extends SkillUpdateEvent {


    public SkillEquipEvent(Player player, Skill skill, RoleBuild roleBuild) {
        super(player, skill, roleBuild);
    }
}
