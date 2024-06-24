package me.mykindos.betterpvp.champions.champions.skills.types;


import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;

public interface ToggleSkill extends IChampionsSkill {


    void toggle(Player player, int level);

}
