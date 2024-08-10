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
    private final Vector initialVelocity;  // Keep this as the original velocity
    private final Vector gravity;
    private final long startTime;
    private final float initialYaw;  // Store the initial yaw

    @Setter
    private Location initialPosition; // This is now the mutable position

    @Setter
    private Vector currentVelocity;  // New mutable velocity

    public AxeData(ItemDisplay axeDisplay, Location originalPosition, Vector initialVelocity, Vector gravity, long startTime, float initialYaw) {
        this.axeDisplay = axeDisplay;
        this.originalPosition = originalPosition;
        this.initialPosition = originalPosition;  // Initialize mutable position to the original
        this.initialVelocity = initialVelocity;
        this.gravity = gravity;
        this.startTime = startTime;
        this.initialYaw = initialYaw;  // Initialize the initial yaw
        this.currentVelocity = initialVelocity;  // Initialize mutable velocity
    }
}
