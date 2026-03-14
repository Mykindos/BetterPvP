package me.mykindos.betterpvp.core.tracking.model;

/**
 * Mutable per-cell accumulator for heat data.
 * All mutations occur on the main server thread via {@link me.mykindos.betterpvp.core.tracking.listeners.ActivityTrackingListener}.
 */
public class HeatCell {

    private double heatValue;
    private double peakHeat;
    private int totalVisits;
    private int combatEvents;

    public void addPresence(double weight) {
        heatValue += weight;
        totalVisits++;
        if (heatValue > peakHeat) {
            peakHeat = heatValue;
        }
    }

    public void addCombat(double weight) {
        heatValue += weight;
        combatEvents++;
        if (heatValue > peakHeat) {
            peakHeat = heatValue;
        }
    }

    public void decay(double factor) {
        heatValue *= factor;
    }

    public double getHeatValue() {
        return heatValue;
    }

    public double getPeakHeat() {
        return peakHeat;
    }

    public int getTotalVisits() {
        return totalVisits;
    }

    public int getCombatEvents() {
        return combatEvents;
    }

}
