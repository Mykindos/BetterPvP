package me.mykindos.betterpvp.clans.champions.skills.events;

import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import org.bukkit.entity.Player;


public class SkillEquipEvent extends SkillUpdateEvent {


    public SkillEquipEvent(Player player, Skill skill, RoleBuild roleBuild) {
        super(player, skill, roleBuild);
    }
}
