package me.mykindos.betterpvp.clans.champions.skills.types;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public interface InteractSkill {

    void activate(Player player, int level);

    Action[] getActions();

    default boolean canUseSlowed(){
        return true;
    }

}
