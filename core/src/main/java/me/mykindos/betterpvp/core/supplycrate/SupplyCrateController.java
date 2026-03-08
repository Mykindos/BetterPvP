package me.mykindos.betterpvp.core.supplycrate;

import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

@Singleton
@PluginAdapter("ModelEngine")
@Getter
public class SupplyCrateController {

    private final Set<SupplyCrate> supplyCrates = new HashSet<>();

    public SupplyCrate spawnSupplyCrate(SupplyCrateType type, Location location, @Nullable Player caller) {
        final SupplyCrate projectile = new SupplyCrate(caller, location, type, 1 * 60 * 1000L);
        projectile.redirect(new Vector(0, -projectile.getType().getFallSpeed(), 0));
        supplyCrates.add(projectile);
        return projectile;
    }

}
