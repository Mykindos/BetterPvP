package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Somewhere a player can be, such as a landmark, a hub, an expedition or a clan's camp.
 * <p>
 * A site says what a place is and how it behaves, never what is in it. Zones, props, NPCs and resource nodes reach a
 * world through the world content pipeline.
 */
@Value
public class Site {

    @NotNull String id;
    @NotNull Component displayName;
    @NotNull Material icon;
    @NotNull WorldSource worldSource;
    @NotNull SitePolicy policy;

    /** How long the crossing to it takes. */
    @NotNull VoyageTiming timing;

    /** The Mapper marker naming this site's landing spots. */
    @NotNull String arrivalMarker;

    /** Which of those spots a party is put down at. */
    @NotNull ArrivalDistribution arrival;

    public @NotNull SiteKey key() {
        return SiteKey.of(id);
    }

    /** The key for one owner's instances of this site. */
    public @NotNull SiteKey keyFor(long ownerId) {
        return SiteKey.of(id, ownerId);
    }
}
