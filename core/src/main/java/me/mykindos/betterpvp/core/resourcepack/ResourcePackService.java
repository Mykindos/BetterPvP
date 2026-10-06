package me.mykindos.betterpvp.core.resourcepack;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilServer;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holds the release this server's channel serves. {@link #reload()} reads the newest one and calls
 * {@link PackReleaseChangedEvent} when it differs from the one held.
 */
@CustomLog
@Singleton
public class ResourcePackService {

    private final Core core;
    private final PackReleaseStore store;
    private final AtomicReference<PackRelease> current = new AtomicReference<>();

    @Getter
    @Inject
    @Config(path = "core.resourcepack.channel", defaultValue = "staging")
    private String channel;

    @Inject
    public ResourcePackService(Core core, PackReleaseStore store) {
        this.core = core;
        this.store = store;
    }

    public Optional<PackRelease> current() {
        return Optional.ofNullable(current.get());
    }

    public void reload() {
        UtilServer.runTaskAsync(core, () -> {
            final Optional<PackRelease> latest;
            try {
                latest = store.latest(channel);
            } catch (Exception ex) {
                log.error("Could not read the {} resource pack release", channel, ex).submit();
                return;
            }
            if (latest.isEmpty()) {
                log.warn("Channel {} has no resource pack release, so no packs are sent", channel).submit();
                return;
            }
            final PackRelease release = latest.get();
            final PackRelease previous = current.getAndSet(release);
            if (previous != null && previous.getVersion().equals(release.getVersion())) {
                return;
            }
            log.info("Serving resource pack release {} of {}", release.getVersion(), channel).submit();
            UtilServer.runTask(core, () -> UtilServer.callEvent(new PackReleaseChangedEvent(previous, release)));
        });
    }

}
