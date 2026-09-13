package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.framework.net.SiteDirectory;
import org.jetbrains.annotations.NotNull;

/**
 * Chooses between placing on one server and placing across a network, and is the only place that choice is made.
 * <p>
 * Networked placement needs a directory several servers can agree on, so it is used only when the directory says it
 * is shared. Without that every server would believe it was the whole network and hand out instances the others knew
 * nothing about.
 */
@Singleton
@CustomLog
public class PlacementProvider implements Provider<Placement> {

    @Inject
    private SiteDirectory directory;

    @Inject
    private Provider<LocalPlacement> local;

    @Inject
    private Provider<NetworkPlacement> network;

    private volatile Placement placement;

    @Override
    public @NotNull Placement get() {
        final Placement existing = placement;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (placement == null) {
                final boolean networked = directory.isShared();
                placement = networked ? network.get() : local.get();
                log.info("Placing parties {}", networked ? "across the network" : "on this server only").submit();
            }
            return placement;
        }
    }
}
