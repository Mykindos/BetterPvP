package me.mykindos.betterpvp.champions.weapons.impl.legendaries.scythe;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.utilities.UtilLocation;
import me.mykindos.betterpvp.core.utilities.math.VectorLine;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
public class ScytheData {

    private final Scythe scythe;
    private final @NotNull Gamer gamer;
    private final ChargeData chargeData = new ChargeData(0);
    private long nextSoulExpiry = -999;
    private long lastHarvestStart = -999;
    private UUID targetSoul = null;
    private boolean harvesting = false;
    private boolean markForRemoval = false;

    public Soul getTargetSoul() {
        return scythe.souls.get(targetSoul);
    }

    public void setTargetSoul(@Nullable Soul soul) {
        if (soul == null) {
            targetSoul = null;
        } else {
            targetSoul = soul.getUniqueId();
        }
    }

    public double getSoulCount() {
        return chargeData.getCharge() * scythe.maxSouls;
    }

    public void setSoulCount(double count) {
        chargeData.setCharge((float) (count / scythe.maxSouls));
    }

    private @NotNull Player getPlayer() {
        return Objects.requireNonNull(gamer.getPlayer());
    }

    public boolean isHarvesting() {
        return targetSoul != null && lastHarvestStart != -999 && harvesting;
    }

    public void startHarvesting() {
        Preconditions.checkNotNull(getTargetSoul(), "Cannot start harvesting without a target");
        harvesting = true;
        getTargetSoul().setHarvesting(true);
        lastHarvestStart = System.currentTimeMillis();
    }

    public boolean attemptHarvest() {
        if (isHarvesting()) {
            if (System.currentTimeMillis() - lastHarvestStart >= scythe.soulHarvestSeconds * 1000) {
                gainSoul(getTargetSoul().getCount());
                scythe.souls.remove(targetSoul); // Remove the soul from the world
                stopHarvesting();
                return true;
            }
        }
        return false;
    }

    public boolean gainSoul(double count) {
        nextSoulExpiry = System.currentTimeMillis() + (long) (scythe.soulExpirySeconds * 1000);
        if (canHarvest()) {
            setSoulCount(Math.min(scythe.maxSouls, getSoulCount() + count));
            return true;
        }
        return false;
    }

    public boolean tryExpireSoul(double count) {
        if (System.currentTimeMillis() >= nextSoulExpiry) {
            setSoulCount(Math.max(0, getSoulCount() - count));
            return true;
        }
        return false;
    }

    public boolean canHarvest() {
        return getSoulCount() < scythe.maxSouls;
    }

    public void stopHarvesting() {
        lastHarvestStart = -999;
        if (getTargetSoul() != null) {
            getTargetSoul().setHarvesting(false);
        }
        harvesting = false;
    }

    public void playPassive() {
        if (getSoulCount() <= 0) {
            return;
        }

        // At level 1 and above, play crit particles around
        final Player player = getPlayer();
        new ParticleBuilder(Particle.SOUL_FIRE_FLAME)
                .count(2)
                .extra(0)
                .offset(0.4, 0, 0.4)
                .location(player.getLocation().add(0, 0.1, 0))
                .receivers(60)
                .spawn();
    }

    public void playHarvestStart() {
        new SoundEffect(Sound.ENTITY_WARDEN_ROAR, 0f, 1f).play(getPlayer().getLocation());
    }

    public void playLoseSoul() {
        new SoundEffect(Sound.ENTITY_WARDEN_SONIC_BOOM, 2, 1).play(getPlayer().getLocation());
    }

    public void playHarvestProgress() {
        // Particle line between player and soul
        final Player player = getPlayer();
        final double pitch = Math.toRadians(player.getLocation().getPitch() + 90);
        final double yaw = Math.toRadians(-player.getLocation().getYaw());
        final Location start = player.getLocation().add(0, player.getHeight() / 2, 0);
        final VectorLine line = VectorLine.withStepSize(start, getTargetSoul().getLocation(), 0.2);

        // Make a randomly offset line that corrects itself to the target
        final Location[] locs = line.toLocations();
        final double time = ((System.currentTimeMillis() / 10d) % 360);
        for (int i = 0; i < locs.length; i++) {
            final Location location = locs[i];
            final double angle = Math.toRadians(time + (i * 10));
            final double pointAmplitude = 2 * ((double) i / locs.length);
            double x1 = Math.cos(angle) * pointAmplitude;
            double z1 = Math.sin(angle) * pointAmplitude;
            final int colorOffset = (int) (((double) i / locs.length) * 140d);

            // Vertical SINE wave rotated around the eye direction at a random angle
            final Vector vector = new Vector(x1, 0, z1);
            vector.rotateAroundX(pitch);
            vector.rotateAroundY(yaw);
            vector.add(location.toVector());
            final Location verticalLoc = vector.toLocation(player.getWorld());
            final Color color = Color.fromRGB(Math.max(0, 203 - colorOffset), Math.max(0, 92 - colorOffset), Math.max(0, 255 - colorOffset));
            verticalLoc.getWorld().spawnParticle(Particle.DUST,
                    verticalLoc,
                    1,
                    new Particle.DustOptions(color, 0.8f));
        }
    }

    public void playHarvest(@Nullable Soul soul) {
        final Player player = getPlayer();
        final Location loc = soul == null ? player.getLocation() : soul.getLocation();

        // Spawn a ring of particles and sonic booms around the player
        final int maxRadiusTicks = 6; // Expand to max radius over half a second
        final int radius = 5;
        new BukkitRunnable() {
            double ticks = 0;
            @Override
            public void run() {
                ticks++;
                if (ticks >= maxRadiusTicks) {
                    cancel();
                    return;
                }

                // Particle ring
                final int colorDelta = (int) ((ticks / maxRadiusTicks) * 25);
                final double particleRadius = ((double) radius / maxRadiusTicks) * ticks;
                for (int degree = 0; degree <= 360; degree += 15) {
                    final Location absRingPoint = UtilLocation.fromFixedAngleDistance(loc, particleRadius, degree);

                    final Optional<Location> ringPoint = UtilLocation.getClosestSurfaceBlock(absRingPoint, 3.0, true);
                    final Location particleLoc = ringPoint.orElse(absRingPoint).add(0.0, 1.1, 0.0);
                    Particle.DUST.builder()
                            .color(203 + colorDelta, 92, 255)
                            .location(particleLoc)
                            .receivers(60)
                            .spawn();
                }
            }
        }.runTaskTimer(JavaPlugin.getPlugin(Champions.class), 0L, 2L);

        // Sounds
        final float pitch = 2 * ((float) getSoulCount() / scythe.maxSouls);
        new SoundEffect(Sound.ENTITY_WARDEN_SONIC_BOOM, pitch, 1).play(player.getLocation());
    }
}