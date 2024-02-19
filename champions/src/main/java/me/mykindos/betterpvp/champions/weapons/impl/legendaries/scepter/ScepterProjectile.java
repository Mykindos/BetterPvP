package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;

@Getter
public abstract class ScepterProjectile {

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
    protected final long expireTime;
    protected Location lastLocation;

    protected ScepterProjectile(Player caster, double hitboxSize, double size, final Location location, long expireTime) {
        this.caster = caster;
        this.hitboxSize = hitboxSize;
        this.size = size;
        this.location = location;
        this.lastLocation = location;
        this.expireTime = expireTime;
    }

    protected void tick() {
        if (!impacted) {
            tryImpact().ifPresentOrElse(result -> impact(result.getHitPosition().toLocation(location.getWorld()), result), this::move);
        }

        onTick();
    }

    protected void move() {
        lastLocation = location.clone();
        if (direction != null) {
            location = location.add(direction);
        }
    }

    private Optional<RayTraceResult> tryImpact() {
        if (UtilTime.elapsed(creationTime, expireTime)) {
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

    public void impact(Location location, RayTraceResult result) {
        Preconditions.checkState(!impacted, "Projectile already impacted");
        this.location = location;
        impacted = true;
        impactTime = System.currentTimeMillis();
        onImpact(location, result);
    }

    protected abstract void onTick();

    protected abstract void onImpact(Location location, RayTraceResult result);

    public void redirect(Vector vector) {
        this.direction = vector.normalize().multiply(speed);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        if (direction != null) {
            direction = direction.normalize().multiply(speed);
        }
    }
}
