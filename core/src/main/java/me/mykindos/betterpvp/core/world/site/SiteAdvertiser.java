package me.mykindos.betterpvp.core.world.site;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.net.RemoteInstance;
import me.mykindos.betterpvp.core.framework.net.SiteDirectory;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Tells the directory what this server is holding, on a heartbeat.
 * <p>
 * Repeating the whole set rather than reporting changes is what makes a server's entries expire when it stops
 * reporting, so a server that dies leaves nothing behind and nothing has to notice that it died.
 */
@BPvPListener
@Singleton
public class SiteAdvertiser implements Listener {

    private final SiteInstances instances;
    private final SiteDirectory directory;

    @Inject
    public SiteAdvertiser(@NotNull SiteInstances instances, @NotNull SiteDirectory directory) {
        this.instances = instances;
        this.directory = directory;
    }

    @UpdateEvent(delay = 10_000)
    public void advertise() {
        final String server = Core.getCurrentRealm().getServer().getName();
        final List<RemoteInstance> held = new ArrayList<>();

        for (SiteInstance instance : instances.all()) {
            held.add(new RemoteInstance(instance.getId(), instance.getKey().getSiteId(),
                    instance.getKey().getOwnerId(), server, instance.getWorldName(),
                    instance.getState() == SiteInstance.State.READY, instance.getOccupants().size()));
        }

        directory.publish(held);
    }
}
