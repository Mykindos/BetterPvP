package me.mykindos.betterpvp.clans.world.camp;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.world.site.SiteKey;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A clan member is respawning at their camp's Barracks. When the camp is loaded here they respawn on {@link #spot},
 * otherwise, or when there is no spot, they travel to the camp and land on {@link #landing}.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CampRespawnEvent extends CustomEvent {

    private final Player player;
    private final SiteKey camp;
    /** The camp's world, or null when it is not loaded here. */
    private final @Nullable World world;
    private @Nullable Location spot;
    private @NotNull String landing;

    public CampRespawnEvent(@NotNull Player player, @NotNull SiteKey camp, @Nullable World world,
                            @Nullable Location spot, @NotNull String landing) {
        this.player = player;
        this.camp = camp;
        this.world = world;
        this.spot = spot;
        this.landing = landing;
    }
}
