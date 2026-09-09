package me.mykindos.betterpvp.core.world.site;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Everything needed to reach one instance, whichever server is holding it.
 */
@Value
public class SiteHandle {

    @NotNull UUID instanceId;
    @NotNull SiteKey key;
    @NotNull String server;
    @NotNull String world;
}
