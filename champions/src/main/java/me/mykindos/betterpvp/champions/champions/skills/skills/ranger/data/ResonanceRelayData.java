package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import lombok.Data;
import org.jetbrains.annotations.Nullable;

@Data
public class ResonanceRelayData {
    private boolean shootBeamNextShot = false;
    private @Nullable ResonanceRelayProjectile projectile = null;

    public void removeOldProjectile() {
        if (projectile != null) {
            projectile.setMarkForRemoval(true);
            projectile = null;
        }
    }
}
