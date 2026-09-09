package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Everything needed to reach one instance, whichever server is holding it.
 */
@Value
public class SiteHandle {

    /**
     * The instance id of a handle that names a server but not yet an instance on it.
     * <p>
     * A party sent to another server needs an instance when they arrive, not before they leave, so the handle can
     * name the destination without one existing yet.
     */
    public static final UUID PENDING = new UUID(0L, 0L);

    @NotNull UUID instanceId;
    @NotNull SiteKey key;
    @NotNull String server;
    @NotNull String world;

    public boolean isPending() {
        return PENDING.equals(instanceId);
    }
}
