package me.mykindos.betterpvp.core.utilities.math;

import java.util.function.DoubleUnaryOperator;

public class VectorParabola extends Function3d {

    public VectorParabola(DoubleUnaryOperator heightFunction) {
        super(Function3d.IDENTITY, heightFunction, Function3d.IDENTITY);
    }

}
