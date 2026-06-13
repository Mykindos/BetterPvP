package me.mykindos.betterpvp.core.quest.primitive;

import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import org.bukkit.entity.Player;

/** Evaluates a condition / requirement primitive for a player. */
@FunctionalInterface
public interface QuestConditionHandler {
    boolean test(Player player, PrimitiveData data);
}
