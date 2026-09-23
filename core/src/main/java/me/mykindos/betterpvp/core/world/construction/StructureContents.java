package me.mykindos.betterpvp.core.world.construction;

import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Something a structure holds that has to go somewhere when the structure is demolished: items in its chests, marker
 * entities standing for stored things, blocks placed into it. Each kind of contents drops itself.
 */
public interface StructureContents {

    /**
     * Drops what {@code structure} held. Called once the structure has already been taken out of its holding, so
     * anything that depends on what the holding still has (a capacity it contributed to) sees it gone.
     *
     * @param at somewhere in the middle of where it stood
     */
    void drop(@NotNull SiteKey site, @NotNull PlacedStructure structure, @NotNull Location at);
}
