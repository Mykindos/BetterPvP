package me.mykindos.betterpvp.clans.world.ship;

import lombok.Value;
import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A moored ship: where its hull was placed, and what that hull covers.
 * <p>
 * The berth is the mooring, not the vessel — the same {@code galleon} structure at two docks is two berths, and each
 * knows only which copy it is. A navigator names one, and standing inside its hull is what makes somebody "on the ship"
 * for the purposes of crewing.
 */
@Value
public class Berth {

    /** The berth marker's {@code id:}, unique within its world. */
    String id;

    String worldName;

    /** The {@code .structure} pasted here. */
    String structure;

    /**
     * The berth marker itself: the spot the structure was pasted against, and the facing the hull was turned to. Its
     * yaw is the hull's real orientation, which is what lets a ship-local offset be laid back onto the vessel.
     */
    Location anchor;

    /**
     * Where a boarding player lands, from the structure's {@code ship_board} marker — on deck, facing the helm.
     * Falls back to the berth marker when the vessel declares none.
     */
    Location board;

    /**
     * What the vessel occupies: the volume its {@code .structure} was captured as, turned and moved to where it was
     * pasted. Null only for an empty mooring, which has no ship and so nothing to stand on.
     */
    @Nullable BoundingBox hull;

    int capacity;

    /**
     * Where the quartermaster stands, from the structure's {@code interact:crew} resident. Null when the vessel has
     * none, which simply means no requests indicator hangs there.
     */
    @Nullable Location quartermaster;

    /** Whether {@code location} is aboard. A hull-less berth contains nobody. */
    public boolean contains(@NotNull Location location) {
        return hull != null
                && location.getWorld() != null
                && location.getWorld().getName().equals(worldName)
                && hull.contains(location.getX(), location.getY(), location.getZ());
    }

    public boolean isCrewable() {
        return hull != null;
    }
}
