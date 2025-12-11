package me.mykindos.betterpvp.champions.item.thornfang;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import me.mykindos.betterpvp.core.utilities.model.projectile.ReturningLinkProjectile;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;

import java.util.Optional;

@Getter
@Setter
public class NeedlegraspProjectile extends ReturningLinkProjectile {

    private final Needlegrasp needlegrasp;
    private final double damage;
    private Location lastCasterLocation;
    private boolean midairHitOccurred = false;

    protected NeedlegraspProjectile(Player caster, Needlegrasp needlegrasp, double damage, double hitboxSize, Location location, long aliveTime, long pullTime, double pullSpeed) {
        super(caster, hitboxSize, location, aliveTime, pullTime, pullSpeed, PullMode.PULL_TO_MIDPOINT, 0.25);
        Preconditions.checkNotNull(caster);
        this.needlegrasp = needlegrasp;
        this.damage = damage;
    }

    @Override
    protected void onTick() {
        // Check for mid-air collision during pull phase
        if (impacted && hit != null && hit.isValid() && !midairHitOccurred) {
            checkMidairCollision();
        }

        super.onTick();
    }

    private void checkMidairCollision() {
        final Optional<RayTraceResult> rayTraceResult = UtilEntity.interpolateCollision(
                lastCasterLocation == null ? caster.getLocation() : lastCasterLocation,
                caster.getLocation(),
                0.6f,
                ent -> ent == hit
        );
        if (rayTraceResult.isPresent()) {
            // Notify Needlegrasp that collision occurred
            if (needlegrasp != null) {
                needlegrasp.markCollisionOccurred(caster, hit);
            }

            // Show particles at collision point
            showCollisionParticles();

            midairHitOccurred = true;
            setMarkForRemoval(true);
        }

        lastCasterLocation = caster.getLocation();
    }

    private void showCollisionParticles() {
        if (hit == null || !hit.isValid()) return;

        // Calculate midpoint between caster and target
        Location midpoint = caster.getLocation().clone()
                .add(hit.getLocation())
                .multiply(0.5);

        // Spawn collision particles
        Particle.BLOCK.builder()
                .count(30)
                .extra(0.1)
                .data(Material.MOSS_BLOCK.createBlockData())
                .offset(0.3, 0.3, 0.3)
                .location(midpoint)
                .receivers(30)
                .spawn();

        Particle.ELECTRIC_SPARK.builder()
                .count(10)
                .extra(0.1)
                .offset(0.3, 0.3, 0.3)
                .location(midpoint)
                .receivers(30)
                .spawn();

        // Play collision sound
        midpoint.getWorld().playSound(midpoint, Sound.BLOCK_BAMBOO_BREAK, 1.0f, 0.8f);
    }

    @Override
    protected void onImpact(Location location, RayTraceResult result) {
        // Fix inverted logic: hitting entity = pull together, no entity = pull caster to wall
        if (result.getHitEntity() != null) {
            setPullMode(PullMode.PULL_TO_MIDPOINT);
        } else {
            setPullMode(PullMode.PULL_CASTER);
        }

        Particle.BLOCK.builder()
                .count(100)
                .extra(0)
                .data(Material.EMERALD_BLOCK.createBlockData())
                .offset(0.5, 0.5, 0.5)
                .location(location)
                .receivers(30)
                .spawn();

        super.onImpact(location, result);
    }

    @Override
    protected Display item() {
        return location.getWorld().spawn(location, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.AIR.createBlockData());
            spawned.setGlowing(false);
            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    protected Display createLink(Location spawnLocation, double height) {
        return spawnLocation.getWorld().spawn(spawnLocation, BlockDisplay.class, spawned -> {
            spawned.setBlock(Material.KELP_PLANT.createBlockData());
            spawned.setGlowing(false);

            Transformation transformation = spawned.getTransformation();
            transformation.getTranslation().set(-0.5, 0, -0.5);
            transformation.getLeftRotation().rotateLocalX((float) Math.toRadians(90));
            transformation.getLeftRotation().rotateLocalZ(0f);
            transformation.getScale().set(0.5, height, 0.5);
            spawned.setTransformation(transformation);

            spawned.setPersistent(false);
            spawned.setTeleportDuration(1);
            spawned.setInterpolationDuration(1);
        });
    }

    @Override
    protected SoundEffect pullSound() {
        return new SoundEffect(Sound.BLOCK_WET_GRASS_BREAK, 0f, 1f);
    }

    @Override
    protected SoundEffect pushSound() {
        return new SoundEffect(Sound.BLOCK_WET_GRASS_PLACE, 2f, 1f);
    }

    @Override
    protected SoundEffect impactSound() {
        return new SoundEffect(Sound.ENTITY_EVOKER_CAST_SPELL, 0.5f, 1f);
    }
}
