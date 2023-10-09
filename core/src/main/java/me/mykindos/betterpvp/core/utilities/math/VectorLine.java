package me.mykindos.betterpvp.core.utilities.math;

import com.google.common.base.Preconditions;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for working with a line of {@link Vector}s.
 */
public class VectorLine {

    private final Location start;
    private final Location end;
    private final double stepSize;

    private VectorLine(final Location start, final Location end, final double stepSize) {
        this.start = start;
        this.end = end;
        this.stepSize = stepSize;
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull VectorLine withStepSize(final @NotNull Location start, final @NotNull Location end, final double stepSize) {
        Preconditions.checkNotNull(start, "start");
        Preconditions.checkNotNull(end, "end");
        Preconditions.checkArgument(stepSize > 0, "stepSize must be greater than 0");
        Preconditions.checkArgument(start.getWorld().equals(end.getWorld()), "Locations are not in the same world");
        return new VectorLine(start, end, stepSize);
    }

    @Contract(value = "_, _, _ -> new", pure = true)
    public static @NotNull VectorLine withSteps(final @NotNull Location start, final @NotNull Location end, final int steps) {
        Preconditions.checkNotNull(start, "start");
        Preconditions.checkNotNull(end, "end");
        Preconditions.checkArgument(steps > 0, "steps must be greater than 0");
        final double distance = start.distance(end);
        final double stepSize = distance / steps;
        return withStepSize(start, end, stepSize);
    }

    public Location getStart() {
        return this.start;
    }

    public Location getEnd() {
        return this.end;
    }

    public Vector[] toVectors() {
        final Vector startingPoint = this.start.toVector();
        final Vector endingPoint = this.end.toVector();
        final Vector direction = endingPoint.subtract(startingPoint).normalize();
        final Vector[] points = new Vector[(int) (this.start.distance(this.end) / this.stepSize)];
        for (int i = 0; i < points.length; i++) {
            points[i] = startingPoint.clone().add(direction.clone().multiply(i * this.stepSize));
        }
        return points;
    }

    public Location[] toLocations() {
        final Vector[] vectors = this.toVectors();
        final Location[] locations = new Location[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            locations[i] = vectors[i].toLocation(this.start.getWorld());
        }
        return locations;
    }
}
