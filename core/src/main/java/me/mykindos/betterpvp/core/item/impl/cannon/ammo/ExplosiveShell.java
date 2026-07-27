package me.mykindos.betterpvp.core.item.impl.cannon.ammo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonConfig;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A primed TNT shell that detonates on contact with a block or a living entity, dealing explosion damage that falls
 * off linearly with distance. Loaded by {@code core:cannonball}.
 */
@Singleton
public class ExplosiveShell implements CannonAmmo {

    private final CannonConfig config;

    @Inject
    private ExplosiveShell(CannonConfig config) {
        this.config = config;
    }

    @Override
    public @NotNull String id() {
        return "explosive";
    }

    @Override
    public @NotNull String itemKey() {
        return "core:cannonball";
    }

    @Override
    public @NotNull Component displayName() {
        return Translations.component("core.item.cannonball.name");
    }

    @Override
    public @NotNull CannonProjectile launch(@NotNull CannonProp cannon, @NotNull Location muzzle,
                                            @NotNull Vector direction, @Nullable UUID shooter) {
        return new CannonProjectile(ShellSupport.spawnShell(muzzle, direction, config.getExplosiveLifeSeconds(), shooter), this, shooter, cannon);
    }

    @Override
    public void tick(@NotNull CannonProjectile projectile) {
        Particle.SMOKE.builder()
                .location(projectile.getEntity().getLocation())
                .extra(0)
                .receivers(60)
                .spawn();
    }

    @Override
    public void onImpact(@NotNull CannonProjectile projectile, @NotNull Location at) {
        ShellSupport.detonate(projectile);
    }

    @Override
    public double damageAt(double distance) {
        return ShellSupport.falloff(distance, config.getExplosiveDamage(), config.getExplosiveMinDamage(),
                config.getExplosiveInnerRadius(), config.getExplosiveOuterRadius());
    }
}
