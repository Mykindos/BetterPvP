package me.mykindos.betterpvp.clans.champions.skills.types;


import me.mykindos.betterpvp.clans.champions.skills.ISkill;
import org.bukkit.entity.Player;

public interface ToggleSkill extends ISkill {


    void toggle(Player player, int level);

}
