package me.mykindos.betterpvp.core.components.champions.events;


import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;


public class PlayerUseInteractSkillEvent extends PlayerUseSkillEvent {

    public PlayerUseInteractSkillEvent(Player player, IChampionsSkill skill, int level) {
        super(player, skill, level);
    }
}
