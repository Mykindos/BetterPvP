package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public interface InteractSkill extends IChampionsSkill {

    void activate(Player player, int level);

    Action[] getActions();

}
