package me.mykindos.betterpvp.champions.item.ability;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.inject.Inject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.customtypes.KeyValue;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import me.mykindos.betterpvp.core.utilities.UtilEntity;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class MagnetismAbility extends ItemAbility {

    private double pullRange;
    private double pullFov;
    private double energyPerTick;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    @EqualsAndHashCode.Exclude
    private final EnergyHandler energyHandler;

    @Inject
    public MagnetismAbility(Champions champions, EnergyHandler energyHandler) {
        super(new NamespacedKey(champions, "magnetism"),
                "Magnetism",
                "Spawn a cone of particles in front of you that pulls entities inwards.",
                TriggerType.HOLD_RIGHT_CLICK);
        this.champions = champions;
        this.energyHandler = energyHandler;
        
        // Default values, will be overridden by config
        this.pullRange = 10.0;
        this.pullFov = 80.3;
        this.energyPerTick = 2.0;
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        
        if (!energyHandler.use(player, getName(), energyPerTick, true)) {
            return false;
        }

        pull(player);
        playCone(player);
        new SoundEffect(Sound.BLOCK_BEACON_DEACTIVATE, 0F, 1F).play(player.getLocation());
        return true;
    }
    
    private void playCone(Player wielder) {
        final float particleStep = 0.25f;
        final double particlePoints = pullRange / particleStep;

        // Calculate the amplitude, in blocks, based on the FOV
        double amplitude = Math.tan(Math.toRadians(pullFov / 2)) * pullRange;

        final World world = wielder.getWorld();
        final Location origin = wielder.getEyeLocation();
        final Vector direction = wielder.getLocation().getDirection().multiply(particleStep); // 0.25 block step size for particles
        final double pitch = Math.toRadians(wielder.getLocation().getPitch() + 90);
        final double yaw = Math.toRadians(-wielder.getLocation().getYaw());

        // Create both rotating vectors
        final double time = ((System.currentTimeMillis() / 10d) % 360);
        for (int i = 0; i < particlePoints; i++) {
            float distance = i * particleStep;
            if (distance < 1) continue; // Skip first 4 particles (too close to player)

            final Location point = origin.clone().add(direction).toLocation(world);
            final double angle = Math.toRadians(time + (i * 10));
            final double pointAmplitude = amplitude * (i / particlePoints);
            double x1 = Math.cos(angle) * pointAmplitude;
            double z1 = Math.sin(angle) * pointAmplitude;

            // First spiral
            Vector spiralPoint = new Vector(x1, 0, z1);
            spiralPoint.rotateAroundX(pitch);
            spiralPoint.rotateAroundY(yaw);
            spiralPoint.add(point.toVector());
            final Location spiralLoc = spiralPoint.toLocation(world);
            new ParticleBuilder(Particle.ENCHANTED_HIT).location(spiralLoc).extra(0).receivers(60).spawn();

            // Second spiral
            Vector spiralPoint2 = new Vector(-x1, 0, -z1);
            spiralPoint2.rotateAroundX(pitch);
            spiralPoint2.rotateAroundY(yaw);
            spiralPoint2.add(point.toVector());
            final Location spiralLoc2 = spiralPoint2.toLocation(world);
            new ParticleBuilder(Particle.ENCHANTED_HIT).location(spiralLoc2).extra(0).receivers(60).spawn();

            origin.add(direction); // Move origin forward
        }
    }

    private void playPullLine(Player wielder, Entity entity) {
        final Location origin = wielder.getEyeLocation().add(wielder.getLocation().getDirection());
        final Location target = entity.getLocation().add(0, entity.getHeight() / 2, 0);
        final VectorLine line = VectorLine.withStepSize(origin, target, 0.5);
        for (Location location : line.toLocations()) {
            new ParticleBuilder(Particle.ASH).location(location).extra(0).receivers(60).spawn();
        }
    }

    private void pull(Player wielder) {
        final Location pullLocation = wielder.getEyeLocation();

        final List<KeyValue<LivingEntity, EntityProperty>> nearby = UtilEntity.getNearbyEntities(wielder, pullLocation, pullRange, EntityProperty.ALL);
        for (KeyValue<LivingEntity, EntityProperty> entry : nearby) {
            final LivingEntity entity = entry.getKey();
            if (entity == wielder) {
                continue; // Skip self
            }

            if (!wielder.hasLineOfSight(entity.getLocation()) && !wielder.hasLineOfSight(entity.getEyeLocation())) {
                continue; // Skip entities not in line of sight
            }

            // Get angle from player to entity
            final double angle = Math.toDegrees(wielder.getLocation().getDirection()
                    .angle(entity.getLocation().toVector().subtract(wielder.getLocation().toVector())));
            if (angle > pullFov / 2) {
                continue; // Skip entities not in front of us
            }

            final Vector trajectory = pullLocation.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(0.3);
            VelocityData velocityData = new VelocityData(trajectory, 0.3, false, 0, 0, 1, true);
            UtilVelocity.velocity(entity, wielder, velocityData);
            playPullLine(wielder, entity);
        }
    }
} 