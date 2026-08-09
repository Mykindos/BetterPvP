package me.mykindos.betterpvp.clans.clans.map.claim;

import lombok.Value;
import org.bukkit.block.BlockFace;

/**
 * One claimed chunk in {@link ClanClaimIndex}: who owns it, and which of its four sides face another chunk owned by the
 * same clan. The renderer needs the second part to draw an outline around a claim block rather than around every chunk.
 * <p>
 * Held once globally rather than once per viewer — the only per-viewer part of a claim is its colour, which is looked
 * up from the relation table at draw time.
 */
@Value
public class ClaimEntry {

    private static final int NORTH = 1;
    private static final int EAST = 1 << 1;
    private static final int SOUTH = 1 << 2;
    private static final int WEST = 1 << 3;

    long clanId;
    /** Bit set of the sides adjoining the same clan, i.e. the sides that are <em>not</em> a border. */
    byte ownedSides;

    public boolean owns(BlockFace face) {
        return (ownedSides & bit(face)) != 0;
    }

    public static byte sides(boolean north, boolean east, boolean south, boolean west) {
        int mask = 0;
        if (north) {
            mask |= NORTH;
        }
        if (east) {
            mask |= EAST;
        }
        if (south) {
            mask |= SOUTH;
        }
        if (west) {
            mask |= WEST;
        }
        return (byte) mask;
    }

    private static int bit(BlockFace face) {
        return switch (face) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> 0;
        };
    }
}
