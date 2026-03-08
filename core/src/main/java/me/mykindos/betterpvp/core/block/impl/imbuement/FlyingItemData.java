package me.mykindos.betterpvp.core.block.impl.imbuement;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import org.bukkit.entity.ItemDisplay;
import org.joml.Vector3f;

/**
 * Data class for tracking flying items
 */
@Getter
@Setter
public class FlyingItemData {
    private ItemDisplay itemDisplay;
    private double angle; // Current angle in circular orbit
    private double baseHeight; // Base height for this item
    private double baseRadius; // Base radius from center
    private double angularSpeed; // Speed of rotation
    private double helixOffset; // Offset for helix motion timing
    private double radiusOffset; // Offset for radius fluctuation timing
    private double radiusFluctuationSpeed; // Speed of radius fluctuation
    private double itemRotationX; // Item's X rotation
    private double itemRotationY; // Item's Y rotation
    private double itemRotationZ; // Item's Z rotation
    private double itemRotationSpeedX; // Speed of X rotation
    private double itemRotationSpeedY; // Speed of Y rotation
    private double itemRotationSpeedZ; // Speed of Z rotation
    private ItemInstance itemInstance; // The item instance this represents
    private Vector3f originalScale; // Original scale for expansion animation

    public FlyingItemData(ItemDisplay itemDisplay, ItemInstance itemInstance) {
        this.itemDisplay = itemDisplay;
        this.itemInstance = itemInstance;
        this.angle = Math.random() * 2 * Math.PI; // Random starting angle
        this.baseHeight = ImbuementPedestalConstants.FLYING_HEIGHT_MIN + Math.random() * (ImbuementPedestalConstants.FLYING_HEIGHT_MAX - ImbuementPedestalConstants.FLYING_HEIGHT_MIN);
        this.baseRadius = ImbuementPedestalConstants.FLYING_RADIUS_MIN + Math.random() * (ImbuementPedestalConstants.FLYING_RADIUS_MAX - ImbuementPedestalConstants.FLYING_RADIUS_MIN);
        this.angularSpeed = 0.1 + Math.random() * 0.1; // Random speed between 0.1 and 0.2 rad/tick
        this.helixOffset = Math.random() * 2 * Math.PI; // Random helix timing offset
        this.radiusOffset = Math.random() * 2 * Math.PI; // Random radius fluctuation timing offset
        this.radiusFluctuationSpeed = 0.05 + Math.random() * 0.05; // Random fluctuation speed
        this.itemRotationX = 0;
        this.itemRotationY = 0;
        this.itemRotationZ = 0;
        this.itemRotationSpeedX = (Math.random() - 0.5) * ImbuementPedestalConstants.ITEM_ROTATION_SPEED * 2; // Random rotation speeds
        this.itemRotationSpeedY = (Math.random() - 0.5) * ImbuementPedestalConstants.ITEM_ROTATION_SPEED * 2;
        this.itemRotationSpeedZ = (Math.random() - 0.5) * ImbuementPedestalConstants.ITEM_ROTATION_SPEED * 2;
        this.originalScale = itemDisplay.getTransformation().getScale();
    }

    // Getter for current radius (base radius + fluctuation)
    public double getCurrentRadius() {
        return baseRadius + Math.sin(radiusOffset) * ImbuementPedestalConstants.RADIUS_FLUCTUATION_AMPLITUDE;
    }
}
