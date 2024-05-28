package me.mykindos.betterpvp.core.components.champions.events;


import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;


public class PlayerUseToggleSkillEvent extends PlayerUseSkillEvent {

    public PlayerUseToggleSkillEvent(Player player, IChampionsSkill skill, int level) {
        super(player, skill, level);
    }
}
