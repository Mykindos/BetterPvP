package me.mykindos.betterpvp.core.item.protection;

import lombok.Value;
import me.mykindos.betterpvp.core.item.ItemInstance;

/**
 * Immutable status object representing a player's current drop-protection state
 * for a single protected item.
 * <p>
 * Instances store the tracked ItemInstance and how many additional drops are
 * required before protection is cleared for that specific item.
 */
@Value
public class DropProtectionStatus {
    /** The ItemInstance being tracked by this protection status. */
    ItemInstance itemInstance;

    /**
     * How many more drops the player must perform with the tracked item before
     * protection is removed (decrements to zero).
     */
    int remainingDrops;
}
