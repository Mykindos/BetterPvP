package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

@Getter
public class AxeData {
    private final ItemDisplay axeDisplay;
    private final Location originalPosition;
    private final Vector initialVelocity;
    private final Vector gravity;
    private final long startTime;
    private final float initialYaw;
    @Setter
    private Location initialPosition;

    @Setter
    private Vector currentVelocity;

    @Setter
    private ItemStack originalItem;

    public AxeData(ItemDisplay axeDisplay, Location originalPosition, Vector initialVelocity, Vector gravity, long startTime, float initialYaw) {
        this.axeDisplay = axeDisplay;
        this.originalPosition = originalPosition;
        this.initialPosition = originalPosition;
        this.initialVelocity = initialVelocity;
        this.gravity = gravity;
        this.startTime = startTime;
        this.initialYaw = initialYaw;
        this.currentVelocity = initialVelocity;
    }
}
