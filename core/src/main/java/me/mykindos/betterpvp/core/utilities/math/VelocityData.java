package me.mykindos.betterpvp.core.utilities.math;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.util.Vector;

@Getter
@Setter
public class VelocityData {

    private Vector vector;
    private double strength;

    boolean setY;
    double baseY;
    double addY;
    double maxY;

    boolean groundBoost;
    boolean resetFallDistance;

    public VelocityData(Vector vector, double strength, boolean setY, double baseY, double addY, double maxY, boolean groundBoost, boolean resetFallDistance) {
        this.vector = vector;
        this.strength = strength;
        this.setY = setY;
        this.baseY = baseY;
        this.addY = addY;
        this.maxY = maxY;
        this.groundBoost = groundBoost;
        this.resetFallDistance = resetFallDistance;
    }

    public VelocityData(Vector vector, double strength, boolean setY, double baseY, double addY, double maxY, boolean groundBoost) {
        this(vector, strength, setY, baseY, addY, maxY, groundBoost, true);
    }

    public VelocityData(Vector vector, double strength, double addY, double maxY, boolean groundBoost) {
        this(vector, strength, false, 0.0D, addY, maxY, groundBoost);
    }

}
