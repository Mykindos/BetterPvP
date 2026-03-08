package me.mykindos.betterpvp.core.supplycrate;

import me.mykindos.betterpvp.core.loot.LootTable;

/**
 * Represents a supply crate that falls from the sky and solidifies once it hits the ground.
 */
public interface SupplyCrateType {

    /**
     * @return The model size of the supply crate. Defaults to 1.
     */
    default double getSize() {
        return 1.0;
    }

    /**
     * @return The speed at which the supply crate falls. Defaults to 1
     */
    default double getFallSpeed() {
        return 5.0;
    }

    /**
     * @return Whether only one supply crate of this type can exist at a time.
     */
    default boolean isUnique() {
        return false;
    }

    /**
     * @return The display name of the supply crate.
     */
    String getDisplayName();

    /**
     * @return The ModelEngine ID of the supply crate.
     */
    String getModelId();

    /**
     * @return The loot table of the supply crate.
     */
    LootTable getLootTable();

}
