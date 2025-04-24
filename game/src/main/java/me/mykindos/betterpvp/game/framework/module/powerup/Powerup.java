package me.mykindos.betterpvp.game.framework.module.powerup;

import me.mykindos.betterpvp.game.framework.model.Lifecycled;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Represents a powerup that can be used by players by walking ontop of it
 */
public interface Powerup extends Lifecycled {

    /**
     * @return The location of the powerup
     */
    Location getLocation();

    /**
     * Called every tick
     */
    default void tick() {
        // ignore
    }

    /**
     * Activates the powerup, if disabled. Can be left empty if the powerup is always enabled
     */
    default void activate() {
        // ignore
    }

    /**
     * Deactivates the powerup, if enabled. Can be left empty if not needed
     */
    default void deactivate() {
        // ignore
    }

    /**
     * @return true if a player can use this powerup
     */
    boolean isEnabled();

    /**
     * Called when a player uses the powerup
     * @param player The player that used the powerup
     */
    void use(Player player);

}
