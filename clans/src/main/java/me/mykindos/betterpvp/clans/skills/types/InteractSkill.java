package me.mykindos.betterpvp.clans.skills.types;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public interface InteractSkill {

    void activate(Player player);

    Action[] getActions();

}
