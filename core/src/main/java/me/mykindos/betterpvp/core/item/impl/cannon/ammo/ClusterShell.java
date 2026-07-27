package me.mykindos.betterpvp.core.item.impl.cannon.ammo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonConfig;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A shell that bursts on impact into several short-fused bomblets scattered around the point of contact, trading the
 * explosive shell's single heavy hit for area denial. Loaded by {@code core:cluster_cannonball}.
 */
@Singleton
public class ClusterShell implements CannonAmmo {

    private final CannonConfig config;

    @Inject
    private ClusterShell(CannonConfig config) {
        this.config = config;
    }

    @Override
    public @NotNull String id() {
        return "cluster";
    }

    @Override
    public @NotNull String itemKey() {
        return "core:cluster_cannonball";
    }

    @Override
    public @NotNull Component displayName() {
        return Translations.component("core.item.cluster_cannonball.name");
    }

    @Override
    public @NotNull CannonProjectile launch(@NotNull CannonProp cannon, @NotNull Location muzzle,
                                            @NotNull Vector direction, @Nullable UUID shooter) {
        return new CannonProjectile(ShellSupport.spawnShell(muzzle, direction, config.getClusterLifeSeconds(), shooter), this, shooter, cannon);
    }

    @Override
    public void tick(@NotNull CannonProjectile projectile) {
        Particle.WHITE_SMOKE.builder()
                .location(projectile.getEntity().getLocation())
                .extra(0)
                .count(2)
                .offset(0.1, 0.1, 0.1)
                .receivers(60)
                .spawn();
    }

    @Override
    public void onImpact(@NotNull CannonProjectile projectile, @NotNull Location at) {
        // The carrier never detonates itself - it only distributes bomblets, so the burst reads as a spray rather
        // than one big blast with extras.
        projectile.getEntity().remove();

        new SoundEffect(Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1f).play(at);
        final double spread = config.getClusterSpread();
        for (int i = 0; i < config.getClusterBomblets(); i++) {
            final Vector scatter = new Vector(
                    UtilMath.randDouble(-spread, spread),
                    UtilMath.randDouble(spread * 0.5, spread * 1.5),
                    UtilMath.randDouble(-spread, spread));
            ShellSupport.spawnShell(at.clone(), scatter, UtilMath.randDouble(0.5, 1.0), projectile.getShooter());
        }
    }

    @Override
    public double damageAt(double distance) {
        return ShellSupport.falloff(distance, config.getClusterDamage(), config.getClusterMinDamage(),
                config.getClusterInnerRadius(), config.getClusterOuterRadius());
    }
}
