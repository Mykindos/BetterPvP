package me.mykindos.betterpvp.clans.champions.builds.menus.events;


import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import org.bukkit.entity.Player;


public class SkillDequipEvent extends SkillUpdateEvent {

    public SkillDequipEvent(Player player, Skill skill, RoleBuild roleBuild) {
        super(player, skill, roleBuild);
    }
}
