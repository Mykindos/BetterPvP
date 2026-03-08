package me.mykindos.betterpvp.core.utilities.model.selector.origin;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * A selector origin based on a living entity.
 * Can optionally use the entity's eye location for more accurate directional targeting.
 *
 * @param entity         the entity to use as the origin
 * @param useEyeLocation whether to use the entity's eye location instead of their feet location
 */
public record EntityOrigin(LivingEntity entity, boolean useEyeLocation) implements SelectorOrigin {

    /**
     * Creates an EntityOrigin using the entity's feet location.
     *
     * @param entity the entity to use as the origin
     */
    public EntityOrigin(LivingEntity entity) {
        this(entity, false);
    }

    @Override
    public World getWorld() {
        return entity.getWorld();
    }

    @Override
    public Vector getPosition() {
        return useEyeLocation ? entity.getEyeLocation().toVector() : entity.getLocation().toVector();
    }

    @Override
    public Location toLocation() {
        return useEyeLocation ? entity.getEyeLocation() : entity.getLocation();
    }

    @Override
    public @NotNull Vector getOrientation() {
        return entity.getLocation().getDirection();
    }

    @Override
    public float getPitch() {
        return entity.getLocation().getPitch();
    }

    @Override
    public float getYaw() {
        return entity.getLocation().getYaw();
    }

    /**
     * Gets the entity this origin is based on.
     *
     * @return the entity
     */
    public LivingEntity getEntity() {
        return entity;
    }
}
