package me.mykindos.betterpvp.core.combat.offhand;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.jetbrains.annotations.NotNull;

/**
 * Triggers offhand actions when a player presses the offhand key.
 */
@FunctionalInterface
public interface OffhandExecutor {

    /**
     * Triggers the offhand action for the given client.
     * @param client the client that triggered the offhand action
     * @param itemInstance the item instance that triggered the offhand action
     * @return true if the action was successfully triggered, false otherwise
     */
    boolean trigger(@NotNull Client client, @NotNull ItemInstance itemInstance);

}
