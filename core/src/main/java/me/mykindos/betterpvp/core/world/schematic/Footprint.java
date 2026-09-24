package me.mykindos.betterpvp.core.world.schematic;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import lombok.Getter;
import me.mykindos.betterpvp.core.world.zone.ZoneBounds;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * The ground a placed structure stands on: the world columns its blocks occupy, and the height range they span.
 * <p>
 * Columns rather than a box, because builds are rarely rectangular. Two L-shaped buildings can nest into each other's
 * corners without their boxes allowing it.
 */
@Getter
public final class Footprint {

    /** Occupied {@code (x, z)} columns, packed with {@link #pack}. */
    private final LongSet columns;
    private final int minY;
    private final int maxY;

    Footprint(@NotNull LongSet columns, int minY, int maxY) {
        this.columns = LongSets.unmodifiable(new LongOpenHashSet(columns));
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }

    /** Whether the two share any column over a shared height, which is what would make their blocks collide. */
    public boolean intersects(@NotNull Footprint other) {
        if (isEmpty() || other.isEmpty() || maxY < other.minY || other.maxY < minY) {
            return false;
        }
        final LongSet smaller = columns.size() <= other.columns.size() ? columns : other.columns;
        final LongSet larger = smaller == columns ? other.columns : columns;
        for (long column : smaller) {
            if (larger.contains(column)) {
                return true;
            }
        }
        return false;
    }

    /** Whether every column lies inside {@code bounds}, at both the bottom and the top of the structure. */
    public boolean within(@NotNull ZoneBounds bounds, @NotNull World world) {
        for (long column : columns) {
            final int x = unpackX(column);
            final int z = unpackZ(column);
            if (!bounds.contains(new Location(world, x + 0.5, minY + 0.5, z + 0.5))
                    || !bounds.contains(new Location(world, x + 0.5, maxY + 0.5, z + 0.5))) {
                return false;
            }
        }
        return true;
    }

    public boolean containsColumn(int x, int z) {
        return columns.contains(pack(x, z));
    }

    public static long pack(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public static int unpackX(long column) {
        return (int) (column >> 32);
    }

    public static int unpackZ(long column) {
        return (int) column;
    }
}
