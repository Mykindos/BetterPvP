package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;

@Getter
public class BlackHole {

    private final Player caster;
    private final double hitboxSize;
    private final double size;
    private final double pullStrength;
    private final double pullRadius;
    private final double aliveSeconds;
    private final double expandSeconds;
    private double speed = 1;
    private Location location;
    private boolean impacted;
    private final long creationTime = System.currentTimeMillis();
    private ChargeData chargeData;
    private List<Location> sphere;
    private long impactTime;
    private Vector direction;
    @Setter
    private boolean markForRemoval;

    public BlackHole(Player caster, final Location location, double hitboxSize, double size, double pullStrength, double pullRadius, double aliveSeconds, double expandSeconds) {
        this.caster = caster;
        this.location = location;
        this.hitboxSize = hitboxSize;
        this.size = size;
        this.pullStrength = pullStrength;
        this.pullRadius = pullRadius;
        this.aliveSeconds = aliveSeconds;
        this.expandSeconds = expandSeconds;
    }

    public void tick() {
        if (!impacted) {
            final Optional<Location> result = tryImpact();
            result.ifPresent(this::impact);
            if (result.isEmpty()) {
            move();
            }

            // Play travel particles
            Particle.REDSTONE.builder()
                    .location(location)
                    .count(1)
                    .extra(0.5)
                    .offset(0.1, 0.1, 0.1)
                    .data(new Particle.DustOptions(Color.fromRGB(64, 138, 199), 2))
                    .receivers(60)
                    .spawn();
            return;
        }

        if (UtilTime.elapsed(impactTime, (long) ((expandSeconds + aliveSeconds) * 1000L))) {
            // Expire if it's been alive for too long
            markForRemoval = true;
            new SoundEffect(Sound.BLOCK_CONDUIT_DEACTIVATE, 0f, 1f).play(location);
        } else {
            // Play impact particles
            final float charge = chargeData.getCharge();
            chargeData.tick();
            chargeData.tickSound(new SoundEffect(Sound.BLOCK_CONDUIT_DEACTIVATE, 2f, 1f), location, true);

            for (Location point : sphere) {
                final Location direction = location.clone().subtract(point);
                direction.multiply(1 - charge);
                final Color color = Math.random() > 0.5 ? Color.fromRGB(37, 78, 112) : Color.fromRGB(64, 138, 199);
                Particle.REDSTONE.builder()
                        .data(new Particle.DustOptions(color, 2))
                        .location(point.clone().add(direction))
                        .count(1)
                        .extra(0)
                        .receivers(60)
                        .spawn();
            }

            Particle.GLOW.builder()
                    .location(location)
                    .count(1)
                    .extra(0.5)
                    .offset(0.5, 0.5, 0.5)
                    .receivers(60)
                    .spawn();

            // Pull entities
            for (LivingEntity entity : location.getNearbyLivingEntities(pullRadius)) {
                if (entity instanceof ArmorStand) {
                    continue;
                }

                final Location entityLocation = entity.getLocation();
                final Vector direction = location.toVector().subtract(entityLocation.toVector()).normalize();
                entity.setFallDistance(0);
                entity.setVelocity(direction.multiply(pullStrength));

                // Pull particles
                for (Location loc : VectorLine.withStepSize(location, entityLocation, 0.2).toLocations()) {
                    Particle.ASH.builder()
                            .location(loc)
                            .count(1)
                            .extra(0)
                            .receivers(60)
                            .spawn();
                }
            }
        }
    }

    private void move() {
        if (direction != null) {
            location = location.add(direction);
        }
    }

    private Optional<Location> tryImpact() {
        if (UtilBlock.solid(location.getBlock()) || UtilTime.elapsed(creationTime, 2_000)) {
            return Optional.ofNullable(location);
        }

        final RayTraceResult rayTrace = location.getWorld().rayTrace(location,
                direction,
                speed,
                FluidCollisionMode.NEVER,
                true,
                hitboxSize,
                entity -> entity != caster && entity instanceof LivingEntity);
        if (rayTrace != null) {
            return Optional.of(rayTrace.getHitPosition().toLocation(location.getWorld()));
        }

        return Optional.empty();
    }

    public void impact(Location location) {
        Preconditions.checkState(!impacted, "Black hole already impacted");
        this.location = location;
        impacted = true;
        impactTime = System.currentTimeMillis();
        sphere = UtilLocation.getSphere(location, size, 6);
        chargeData = new ChargeData((float) (1 / expandSeconds));
    }

    public void redirect(Vector vector) {
        this.direction = vector.normalize().multiply(speed);
        new SoundEffect(Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0f, 1f).play(location);
    }

    public void setSpeed(double speed) {
        this.speed = speed;
        if (direction != null) {
            direction = direction.normalize().multiply(speed);
        }
    }

}
