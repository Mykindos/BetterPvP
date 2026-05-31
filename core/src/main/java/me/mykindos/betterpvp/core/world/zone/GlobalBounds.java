package me.mykindos.betterpvp.core.world.zone;

import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Ambient {@link ZoneBounds} defined by a predicate over a world (or all worlds when {@code world} is {@code null}).
 * Use for areas that are not chunk-aligned, e.g. "standing in water" or "anywhere in this world". Because it reports
 * no covered chunks, the {@link ZoneManager} evaluates it on every resolution rather than via the index, so keep the
 * number of ambient zones small.
 */
public final class GlobalBounds implements ZoneBounds {

    private final World world;
    private final Predicate<Location> predicate;

    public GlobalBounds(@Nullable World world, @NotNull Predicate<Location> predicate) {
        this.world = world;
        this.predicate = predicate;
    }

    /**
     * @param world the world this zone blankets
     * @return a bounds that contains every location in the given world
     */
    public static GlobalBounds world(@NotNull World world) {
        return new GlobalBounds(world, location -> true);
    }

    @Override
    public boolean contains(@NotNull Location location) {
        return (world == null || location.getWorld() == world) && predicate.test(location);
    }

    @Override
    public @Nullable World getWorld() {
        return world;
    }

    @Override
    public @NotNull LongSet coveredChunks() {
        return LongSets.EMPTY_SET;
    }
}
