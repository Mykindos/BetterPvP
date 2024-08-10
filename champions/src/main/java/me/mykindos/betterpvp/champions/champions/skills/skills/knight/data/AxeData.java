package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

@Getter
public class AxeData {
    private final ItemDisplay axeDisplay;
    private final Location originalPosition;  // Renamed from 'initialPosition'
    private final Vector initialVelocity;
    private final Vector acceleration;
    private final long startTime;

    @Setter
    private Location initialPosition; // This is now the mutable position

    public AxeData(ItemDisplay axeDisplay, Location originalPosition, Vector initialVelocity, Vector acceleration, long startTime) {
        this.axeDisplay = axeDisplay;
        this.originalPosition = originalPosition;
        this.initialPosition = originalPosition;  // Initialize mutable position to the original
        this.initialVelocity = initialVelocity;
        this.acceleration = acceleration;
        this.startTime = startTime;
    }
}
