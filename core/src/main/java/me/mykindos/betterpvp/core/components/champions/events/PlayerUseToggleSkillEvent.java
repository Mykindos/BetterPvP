package me.mykindos.betterpvp.core.components.champions.events;


import me.mykindos.betterpvp.core.components.champions.ISkill;
import org.bukkit.entity.Player;


public class PlayerUseToggleSkillEvent extends PlayerUseSkillEvent {

    public PlayerUseToggleSkillEvent(Player player, ISkill skill, int level) {
        super(player, skill, level);
    }
}
