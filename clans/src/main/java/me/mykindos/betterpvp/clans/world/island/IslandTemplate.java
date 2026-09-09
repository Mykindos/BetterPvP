package me.mykindos.betterpvp.clans.world.island;

import lombok.Value;
import me.mykindos.betterpvp.core.world.site.VoyageTiming;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * A hand-authored discovery island template: a world folder on disk that {@link IslandWorldProvisioner} clones to
 * produce a fresh {@link IslandInstance}.
 */
@Value
public class IslandTemplate {

    @NotNull String key;
    @NotNull Component displayName;
    @NotNull String templateFolder;
    @NotNull Material icon;

    /** How long the crossing to it takes, since an island is reached by ship like anywhere else. */
    @NotNull VoyageTiming timing;

}
