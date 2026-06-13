package me.mykindos.betterpvp.core.combat.offhand;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Triggers offhand actions when a player presses the offhand key.
 */
@FunctionalInterface
public interface OffhandExecutor {

    /**
     * Triggers the offhand action for the given client.
     * @param client the client that triggered the offhand action
     * @param itemInstance the custom item in the offhand, or null if the offhand
     *                     is empty or holds a non-custom item
     * @return true if the press was consumed, false to let lower-priority executors try
     */
    boolean trigger(@NotNull Client client, @Nullable ItemInstance itemInstance);

}
