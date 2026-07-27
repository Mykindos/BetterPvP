package me.mykindos.betterpvp.core.item.impl.cannon.ammo;

import lombok.experimental.UtilityClass;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Shared launch and falloff maths for the TNT-backed shells. Plain functions rather than a shared base class, so ammo
 * types stay flat single-purpose singletons and a non-TNT projectile can ignore all of it.
 */
@UtilityClass
public class ShellSupport {

    /**
     * Spawns a primed-TNT shell that will not detonate on its own before {@code lifeSeconds} elapse, tagged so damage
     * crediting and the cannon's own collision filters can recognise it.
     */
    public static @NotNull TNTPrimed spawnShell(@NotNull Location muzzle, @NotNull Vector velocity,
                                                double lifeSeconds, @Nullable UUID shooter) {
        final TNTPrimed shell = muzzle.getWorld().spawn(muzzle, TNTPrimed.class);
        if (shooter != null) {
            final Player source = Bukkit.getPlayer(shooter);
            if (source != null) {
                shell.setSource(source);
            }
            shell.getPersistentDataContainer().set(CoreNamespaceKeys.ORIGINAL_OWNER, CustomDataType.UUID, shooter);
        }
        shell.getPersistentDataContainer().set(CoreNamespaceKeys.ENTITY_TYPE, PersistentDataType.STRING, "cannonball");
        shell.setVelocity(velocity);
        shell.setFuseTicks((int) Math.max(1, lifeSeconds * 20L));
        return shell;
    }

    /**
     * Linear damage falloff between an inner radius (full damage) and an outer radius (nothing), floored at
     * {@code minDamage} anywhere inside the outer radius.
     */
    public static double falloff(double distance, double maxDamage, double minDamage,
                                 double innerRadius, double outerRadius) {
        if (distance <= innerRadius) {
            return maxDamage;
        }
        if (distance > outerRadius) {
            return 0;
        }
        final double band = outerRadius - innerRadius;
        if (band <= 0) {
            return maxDamage;
        }
        return Math.max(maxDamage * ((band - (distance - innerRadius)) / band), minDamage);
    }

    /** Detonates a shell immediately by burning its remaining fuse down to zero. */
    public static void detonate(@NotNull CannonProjectile projectile) {
        if (projectile.getEntity() instanceof TNTPrimed tnt && tnt.isValid()) {
            tnt.setFuseTicks(0);
        }
    }
}
