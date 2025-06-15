package me.mykindos.betterpvp.core.block.behavior;

import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a behavior that can be triggered by clicking on a block.
 * This interface extends {@link SmartBlockBehavior} to define click-specific behaviors.
 */
@FunctionalInterface
public interface ClickBehavior extends SmartBlockBehavior {

    void trigger(@NotNull SmartBlockInstance instance, @NotNull Player player);

}
