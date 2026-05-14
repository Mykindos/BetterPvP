package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import lombok.Data;
import org.bukkit.entity.Arrow;
import org.jetbrains.annotations.Nullable;

@Data
public class ResonanceRelayData {
    private boolean shootBeamNextShot = false;
    private @Nullable ResonanceRelayProjectile projectile = null;
    private @Nullable Arrow shotArrow = null;

    public void removeProjectileAndArrow() {
        if (projectile != null) {
            projectile.setMarkForRemoval(true);
            projectile = null;
        }

        if (shotArrow != null) {
            shotArrow.remove();
            shotArrow = null;
        }
    }
}
