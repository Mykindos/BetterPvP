package me.mykindos.betterpvp.clans.world.camp.structure;

import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.world.construction.PlacedStructure;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What a fitted upgrade shows or lets a member do when they use it: from its entry on the Steward's upgrades page, or
 * by right-clicking its piece on the structure.
 */
@FunctionalInterface
public interface UpgradePage {

    /** @param previous where Back returns to, or null when it was opened from the piece in the world */
    void open(@NotNull Player player, @NotNull SiteKey camp, @NotNull PlacedStructure structure,
              @Nullable Windowed previous);
}
