package me.mykindos.betterpvp.core.utilities.model;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

@Getter
public abstract class RayProjectile {

    protected final Player caster;
    protected final double hitboxSize;
    protected final double size;
    protected final long creationTime = System.currentTimeMillis();
    protected Location location;
    protected double speed = 1;
    protected boolean impacted;
    protected long impactTime;
    protected Vector direction;
    @Setter
    protected boolean markForRemoval;
    protected final long aliveTime;
    protected Location lastLocation;

    protected RayProjectile(@Nullable Player caster, double hitboxSize, double size, final Location location, long aliveTime) {
        this.caster = caster;
        this.hitboxSize = hitboxSize;
        this.size = size;
        this.location = location;
        this.lastLocation = location;
        this.aliveTime = aliveTime;
    }

    public boolean isExpired() {
        return UtilTime.elapsed(creationTime, aliveTime);
    }

    protected Location[] interpolateLine() {
        return interpolateLine(0.5);
    }

    protected Location[] interpolateLine(double step) {
        return lastLocation == null || lastLocation.equals(location) || lastLocation.distanceSquared(location) < step * step
                ? new Location[] { location }
                : VectorLine.withStepSize(lastLocation, location, step).toLocations();
    }

    public void tick() {
        if (!impacted) {
            final Optional<RayTraceResult> result = checkCollision();
            if (result.isPresent()) {
                final CollisionResult collisionResult = onCollide(result.get());
                switch (collisionResult) {
                    case IMPACT:
                        impact(result.get().getHitPosition().toLocation(location.getWorld()), result.get());
                        break;
                    case REFLECT_BLOCKS:
                        if (result.get().getHitBlock() != null) {
                            final Vector normal = Objects.requireNonNull(result.get().getHitBlockFace()).getOppositeFace().getDirection();
                            this.location = result.get().getHitPosition().toLocation(location.getWorld());
                            this.direction = this.direction.subtract(normal.multiply(2 * this.direction.dot(normal)));
                        } else {
                            move();
                        }
                        break;
                    case CONTINUE:
                        move();
                        break;
                }
            } else {
                move();
            }

            onTick();
            return;
        }

        move();
        onTick();
    }

    private void move(Location newLocation) {
        this.lastLocation = this.location.clone();
        this.location = newLocation;
    }

    private void move() {
        if (direction != null) {
            this.move(location.clone().add(direction));
        } else {
            this.move(location);
        }
    }

    protected CollisionResult onCollide(RayTraceResult result) {
        return CollisionResult.IMPACT;
    }

    private Optional<RayTraceResult> checkCollision() {
        if (isExpired()) {
            return Optional.of(new RayTraceResult(location.toVector()));
        }

        final RayTraceResult rayTrace = location.getWorld().rayTrace(location,
                direction,
                speed,
                FluidCollisionMode.NEVER,
                true,
                hitboxSize,
                entity -> entity != caster && entity instanceof LivingEntity);
        if (rayTrace != null) {
            return Optional.of(rayTrace);
        }

        return Optional.empty();
    }

    public final void impact() {
        impact(location, new RayTraceResult(location.toVector()));
    }

    public final void impact(Location location, RayTraceResult result) {
        Preconditions.checkState(!impacted, "Projectile already impacted");
        this.location = location;
        this.lastLocation = location;
        impacted = true;
        impactTime = System.currentTimeMillis();
        onImpact(location, result);
    }

    protected abstract void onTick();

    protected abstract void onImpact(Location location, RayTraceResult result);

    public void redirect(Vector vector) {
        this.direction = vector == null ? null : vector.normalize().multiply(speed);
    }

    public final void setSpeed(double speed) {
        this.speed = speed;
        if (direction != null) {
            direction = direction.normalize().multiply(speed);
        }
    }

    public enum CollisionResult {
        IMPACT,
        CONTINUE,
        REFLECT_BLOCKS
    }

}
