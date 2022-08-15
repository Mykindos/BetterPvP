package me.mykindos.betterpvp.champions.champions.skills.types;


import me.mykindos.betterpvp.core.components.champions.ISkill;
import org.bukkit.entity.Player;

public interface ToggleSkill extends ISkill {


    void toggle(Player player, int level);

}
