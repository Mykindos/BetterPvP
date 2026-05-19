package me.mykindos.betterpvp.core.displayname;

import org.bukkit.entity.Entity;

public interface DisplayNameProvider {

    /**
     * Get the display name of an entity relative to a viewer
     */
    String getDisplayName(final Entity entity, final Entity viewer);
}