package me.mykindos.betterpvp.clans.champions.skills.events;


import me.mykindos.betterpvp.clans.champions.skills.Skill;
import org.bukkit.entity.Player;


public class PlayerUseInteractSkillEvent extends PlayerUseSkillEvent {

    public PlayerUseInteractSkillEvent(Player player, Skill skill, int level) {
        super(player, skill, level);
    }
}
