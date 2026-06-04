package me.mykindos.betterpvp.clans.world.resource;

import com.google.inject.Singleton;
import lombok.Getter;
import lombok.Setter;

/**
 * A server-wide respawn speed multiplier for resource nodes, applied on top of per-node delays. World events (e.g.
 * Mining Madness) raise it for a limited time to make ore fields respawn faster, then reset it to 1.0 — this replaces
 * the old {@code Fields#setBonusSpeedMultiplier} hook.
 */
@Singleton
public class ResourceNodeSpeed {

    @Getter
    @Setter
    private double bonusMultiplier = 1.0;
}
