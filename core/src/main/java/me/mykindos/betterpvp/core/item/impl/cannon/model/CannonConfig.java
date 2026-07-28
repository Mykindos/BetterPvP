package me.mykindos.betterpvp.core.item.impl.cannon.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

/**
 * Every tunable number the cannon system has, read from {@code items/weapon.yml}.
 * <p>
 * Values are cached and invalidated by {@link #reload()}; the re-read happens on the next access rather than in the
 * hook itself, so callers reading during a reload pass always see current values regardless of hook ordering.
 */
@Singleton
public class CannonConfig implements Reloadable {

    private final Core core;
    private volatile boolean dirty = true;

    private double cannonHealth;
    private double spawnCooldown;
    private double fuseSeconds;
    private double shootCooldownSeconds;
    private double launcherFuseSeconds;
    private double launcherCooldownSeconds;

    private double explosiveLifeSeconds;
    private double explosiveDamage;
    private double explosiveMinDamage;
    private double explosiveInnerRadius;
    private double explosiveOuterRadius;

    private double clusterLifeSeconds;
    private int clusterBomblets;
    private double clusterSpread;
    private double clusterDamage;
    private double clusterMinDamage;
    private double clusterInnerRadius;
    private double clusterOuterRadius;

    private double rideTargetingSeconds;
    private double rideMaxRange;
    private int rideMinFlightTicks;
    private int rideMaxFlightTicks;

    @Inject
    private CannonConfig(Core core) {
        this.core = core;
    }

    /** Invalidates the cache. Values are re-read on next access. */
    @Override
    public void reload() {
        this.dirty = true;
    }

    private synchronized void ensureFresh() {
        if (!dirty) {
            return;
        }
        final ExtendedYamlConfiguration config = core.getConfig("items/weapon");

        cannonHealth = getDouble(config, "cannon.health", 200.0);
        spawnCooldown = getDouble(config, "cannon.spawn-cooldown", 30.0);
        fuseSeconds = getDouble(config, "cannon.fuse-seconds", 2.5);
        shootCooldownSeconds = getDouble(config, "cannon.shoot-cooldown", 15.0);
        launcherFuseSeconds = getDouble(config, "cannon.launcher.fuse-seconds", 3.5);
        launcherCooldownSeconds = getDouble(config, "cannon.launcher.cooldown", 8.0);

        explosiveLifeSeconds = getDouble(config, "cannon.cannonball-alive-seconds", 2.0);
        explosiveDamage = getDouble(config, "cannon.cannonball-damage", 15.0);
        explosiveMinDamage = getDouble(config, "cannon.cannonball-min-damage", 4.0);
        explosiveInnerRadius = getDouble(config, "cannon.cannonball-damage-min-radius", 1.0);
        explosiveOuterRadius = getDouble(config, "cannon.cannonball-damage-max-radius", 4.0);

        clusterLifeSeconds = getDouble(config, "cannon.cluster.alive-seconds", 2.0);
        clusterBomblets = config.getOrSaveInt("cannon.cluster.bomblets", 4);
        clusterSpread = getDouble(config, "cannon.cluster.bomblet-spread", 0.35);
        clusterDamage = getDouble(config, "cannon.cluster.damage", 7.0);
        clusterMinDamage = getDouble(config, "cannon.cluster.min-damage", 2.0);
        clusterInnerRadius = getDouble(config, "cannon.cluster.damage-min-radius", 1.0);
        clusterOuterRadius = getDouble(config, "cannon.cluster.damage-max-radius", 3.0);

        rideTargetingSeconds = getDouble(config, "cannon.ride.targeting-seconds", 15.0);
        rideMaxRange = getDouble(config, "cannon.ride.max-range", 120.0);
        rideMinFlightTicks = config.getOrSaveInt("cannon.ride.min-flight-ticks", 25);
        rideMaxFlightTicks = config.getOrSaveInt("cannon.ride.max-flight-ticks", 110);

        dirty = false;
    }

    private static double getDouble(ExtendedYamlConfiguration config, String path, double defaultValue) {
        return config.getOrSaveObject(path, defaultValue, Double.class);
    }

    public double getCannonHealth() {
        ensureFresh();
        return cannonHealth;
    }

    public double getSpawnCooldown() {
        ensureFresh();
        return spawnCooldown;
    }

    public double getFuseSeconds() {
        ensureFresh();
        return fuseSeconds;
    }

    public double getShootCooldownSeconds() {
        ensureFresh();
        return shootCooldownSeconds;
    }

    public double getLauncherFuseSeconds() {
        ensureFresh();
        return launcherFuseSeconds;
    }

    public double getLauncherCooldownSeconds() {
        ensureFresh();
        return launcherCooldownSeconds;
    }

    public double getExplosiveLifeSeconds() {
        ensureFresh();
        return explosiveLifeSeconds;
    }

    public double getExplosiveDamage() {
        ensureFresh();
        return explosiveDamage;
    }

    public double getExplosiveMinDamage() {
        ensureFresh();
        return explosiveMinDamage;
    }

    public double getExplosiveInnerRadius() {
        ensureFresh();
        return explosiveInnerRadius;
    }

    public double getExplosiveOuterRadius() {
        ensureFresh();
        return explosiveOuterRadius;
    }

    public double getClusterLifeSeconds() {
        ensureFresh();
        return clusterLifeSeconds;
    }

    public int getClusterBomblets() {
        ensureFresh();
        return clusterBomblets;
    }

    public double getClusterSpread() {
        ensureFresh();
        return clusterSpread;
    }

    public double getClusterDamage() {
        ensureFresh();
        return clusterDamage;
    }

    public double getClusterMinDamage() {
        ensureFresh();
        return clusterMinDamage;
    }

    public double getClusterInnerRadius() {
        ensureFresh();
        return clusterInnerRadius;
    }

    public double getClusterOuterRadius() {
        ensureFresh();
        return clusterOuterRadius;
    }

    public double getRideTargetingSeconds() {
        ensureFresh();
        return rideTargetingSeconds;
    }

    public double getRideMaxRange() {
        ensureFresh();
        return rideMaxRange;
    }

    public int getRideMinFlightTicks() {
        ensureFresh();
        return rideMinFlightTicks;
    }

    public int getRideMaxFlightTicks() {
        ensureFresh();
        return rideMaxFlightTicks;
    }
}
