package me.mykindos.betterpvp.clans.champions.skills.types;

import me.mykindos.betterpvp.clans.champions.skills.ISkill;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public interface InteractSkill extends ISkill {

    void activate(Player player, int level);

    Action[] getActions();

    default boolean canUseSlowed(){
        return true;
    }

}
