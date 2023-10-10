package me.mykindos.betterpvp.core.utilities.math;

import com.google.common.base.Preconditions;
import org.joml.Vector3d;

import java.util.function.DoubleUnaryOperator;

public class Function3d {

    public static final DoubleUnaryOperator IDENTITY = abs -> abs;

    private final DoubleUnaryOperator x;
    private final DoubleUnaryOperator y;
    private final DoubleUnaryOperator z;

    public Function3d(DoubleUnaryOperator x, DoubleUnaryOperator y, DoubleUnaryOperator z) {
        Preconditions.checkNotNull(x, "x");
        Preconditions.checkNotNull(y, "y");
        Preconditions.checkNotNull(z, "z");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Generate a vector at the desired distance in <b>all axis</b>.
     * @return The generated vector
     */
    public Vector3d atDistance(double distance) {
        return new Vector3d(x.applyAsDouble(distance), y.applyAsDouble(distance), z.applyAsDouble(distance));
    }

}
