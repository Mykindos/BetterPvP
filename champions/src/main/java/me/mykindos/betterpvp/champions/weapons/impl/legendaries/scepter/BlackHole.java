package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scepter;

import lombok.Getter;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.RayProjectile;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

@Getter
public class BlackHole extends RayProjectile {

    private final double pullStrength;
    private final double pullRadius;
    private final double aliveSeconds;
    private final double expandSeconds;
    private ChargeData chargeData;
    private List<Location> sphere;

    public BlackHole(Player caster, final Location location, double hitboxSize, double size, double pullStrength, double pullRadius, double aliveSeconds, double expandSeconds, long expireTime) {
        super(caster, hitboxSize, size, location, expireTime);
        this.pullStrength = pullStrength;
        this.pullRadius = pullRadius;
        this.aliveSeconds = aliveSeconds;
        this.expandSeconds = expandSeconds;
    }

    @Override
    protected void onTick() {
        if (!impacted) {
            // Play travel particles
            for (Location point : interpolateLine()) {
                // Play travel particles
                Particle.DUST.builder()
                        .location(point)
                        .count(1)
                        .extra(0.5)
                        .offset(0.1, 0.1, 0.1)
                        .data(new Particle.DustOptions(Color.fromRGB(64, 138, 199), 2))
                        .receivers(60)
                        .spawn();
            }
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

            var particleReceivers = location.getNearbyPlayers(60);

            for (Location point : sphere) {
                final Location direction = location.clone().subtract(point);
                direction.multiply(1 - charge);
                final Color color = Math.random() > 0.5 ? Color.fromRGB(37, 78, 112) : Color.fromRGB(64, 138, 199);
                Particle.DUST.builder()
                        .data(new Particle.DustOptions(color, 2))
                        .location(point.clone().add(direction))
                        .count(1)
                        .extra(0)
                        .receivers(particleReceivers)
                        .spawn();
            }

            Particle.GLOW.builder()
                    .location(location)
                    .count(1)
                    .extra(0.5)
                    .offset(0.5, 0.5, 0.5)
                    .receivers(particleReceivers)
                    .spawn();

            // Pull entities
            for (KeyValue<LivingEntity, EntityProperty> nearbyEnt : UtilEntity.getNearbyEntities(caster, location, pullRadius, EntityProperty.ALL)) {
                LivingEntity entity = nearbyEnt.getKey();
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

    @Override
    public void redirect(Vector vector) {
        super.redirect(vector);
        new SoundEffect(Sound.ENTITY_WARDEN_ATTACK_IMPACT, 0f, 1f).play(location);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        sphere = UtilLocation.getSphere(location, size, 6);
        chargeData = new ChargeData((float) (1 / expandSeconds));
        setSpeed(0);
    }
}
