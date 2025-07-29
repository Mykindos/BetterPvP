package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.Data;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

import java.util.Objects;

@Data
public class HealthBar {

    private final TextDisplay display;
    private final LivingEntity entity;
    private final ActiveModel model;
    private final ModelBone bone;

    private float lastPercentage = 100;

    public void update() {
        if (bone.getActiveModel().isRemoved()) {
            return;
        }

        // Update health if alive
        if (isValid()) {
            double maxHealth = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getValue();
            float percentage = (float) (entity.getHealth() / maxHealth);
            if(percentage != lastPercentage) {
                display.text(ProgressBar.withLength(percentage, 7).build());
                lastPercentage = percentage;
            }
        }

        // Teleport to entity
        final Location location = bone.getLocation();
        display.setTeleportDuration(1);
        display.teleportAsync(location);
    }

    public boolean isValid() {
        return display != null && display.isValid() && entity != null && entity.isValid();
    }

    public void despawn() {
        if (display == null || !display.isValid()) {
            return;
        }
        display.remove();
    }

}
