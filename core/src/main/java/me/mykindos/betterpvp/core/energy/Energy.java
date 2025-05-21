package me.mykindos.betterpvp.core.energy;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Energy {
    private double current;

    private double max;

    public void setCurrent(double current) {
        this.current = Math.min(Math.max(0, current), max);
    }

    public void addEnergy(double toAdd) {
        setCurrent(this.current + toAdd);
    }

    public void reduceEnergy(double toReduce) {
        setCurrent(this.current - toReduce);
    }

    public void setMax(double max) {
        this.max = max;
        if (this.current > max) {
            current = max;
        }
    }

    @Override
    public String toString() {
        return "Energy(" +
                "current=" + current +
                ", max=" + max +
                ')';
    }
}
