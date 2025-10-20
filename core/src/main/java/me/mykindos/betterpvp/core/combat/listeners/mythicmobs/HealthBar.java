package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import com.ticxo.modelengine.api.entity.BukkitEntity;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.bone.ModelBone;
import lombok.Getter;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.model.ProgressBar;
import me.mykindos.betterpvp.core.utilities.model.ProgressColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;

import java.util.Objects;

@Getter
public class HealthBar {

    private final TextDisplay display;
    private final ActiveModel model;
    private final ModelBone bone;
    private final Vector delta;

    private float lastPercentage = 100;

    public HealthBar(ActiveModel model, ModelBone bone) {
        this.model = model;
        this.bone = bone;

        final Location entityLocation = getEntity().getLocation();
        final Location boneLocation = bone.getLocation();
        this.delta = boneLocation.toVector().subtract(entityLocation.toVector());

        this.display = getEntity().getWorld().spawn(boneLocation, TextDisplay.class, ent -> {
            ent.setPersistent(false);
            ent.setBillboard(Display.Billboard.CENTER);
            ent.setTeleportDuration(1);
            ent.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        });

        UtilEntity.setViewRangeBlocks(display, 10);
    }

    private LivingEntity getEntity() {
        return (LivingEntity) ((BukkitEntity) this.model.getModeledEntity().getBase()).getOriginal();
    }

    public void update() {
        final LivingEntity entity = this.getEntity();
        if (entity == null || !entity.getWorld().isPositionLoaded(entity.getLocation())) {
            return;
        }

        if (bone.getActiveModel().isRemoved()) {
            return; // Stop if model is removed or chunk is not loaded
        }

        // Update health if alive
        if (isValid()) {
            double maxHealth = Objects.requireNonNull(entity.getAttribute(Attribute.MAX_HEALTH)).getValue();
            float percentage = (float) (entity.getHealth() / maxHealth);
            if(percentage != lastPercentage) {
                final TextComponent health = Component.text((int) entity.getHealth(), ProgressColor.of(percentage).getTextColor());
                final TextComponent bar = ProgressBar.withLength(percentage, 7).build();
                display.text(health.appendSpace().append(bar));
                lastPercentage = percentage;
            }
        }

        // Teleport to entity
        final Location location = entity.getLocation().add(delta);
        display.teleport(location);
    }

    public boolean isValid() {
        final LivingEntity entity = this.getEntity();
        return display != null && display.isValid() && entity != null && entity.isValid();
    }

    public void despawn() {
        if (display == null || !display.isValid()) {
            return;
        }
        display.remove();
    }

}
