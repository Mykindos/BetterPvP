package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.components.champions.ISkill;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

public interface InteractSkill extends ISkill {

    void activate(Player player, int level);

    Action[] getActions();

    default boolean canUseSlowed() {
        return true;
    }

    default boolean canUseLevitating() {
        return true;
    }

}
