package me.mykindos.betterpvp.core.quest.primitive;

import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import org.bukkit.entity.Player;

/** Executes an action / reward primitive for a player. */
@FunctionalInterface
public interface QuestActionHandler {
    void run(Player player, PrimitiveData data);
}
