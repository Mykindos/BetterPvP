package me.mykindos.betterpvp.champions.champions.skills.types;

import me.mykindos.betterpvp.core.components.champions.IChampionsSkill;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;

import java.util.concurrent.CompletableFuture;

public interface InteractSkill extends IChampionsSkill {

    /**
     * Activates the skill for the given player and level. The CompletableFuture should complete with true if the skill was successfully activated, or false if it failed to activate (e.g. due to cooldown, insufficient resources, etc.).
     *
     * @param player
     * @param level
     * @return
     */
    CompletableFuture<Boolean> activate(Player player, int level);

    Action[] getActions();

}
