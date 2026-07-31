package me.mykindos.betterpvp.clans.world.island;

import lombok.Value;
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

}
