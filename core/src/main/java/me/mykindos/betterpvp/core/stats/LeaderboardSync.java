package me.mykindos.betterpvp.core.stats;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.net.BusMessage;
import me.mykindos.betterpvp.core.framework.net.MessageBus;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps every server's top ten current when the standing changed somewhere else.
 * <p>
 * The rows themselves are already shared, since a leaderboard reads them from the database rather than from the
 * server it happens to be running on. What is local is the copy each server holds between refreshes, so what travels
 * is which leaderboard moved and not what it now says. The server told re-reads it and arrives at the same answer.
 */
@Singleton
@CustomLog
public class LeaderboardSync {

    static final String TOPIC = "stats.leaderboard";

    private static final String NAME = "name";

    /**
     * How long a server waits before re-reading. The standing changed in memory before the row that caused it was
     * written, so re-reading immediately would find the old numbers.
     */
    private static final long REFRESH_DELAY_TICKS = 100L;

    /** The least time between two announcements of the same leaderboard, so a busy one cannot flood the rest. */
    private static final long ANNOUNCE_INTERVAL_MILLIS = 30_000L;

    private final Core core;
    private final MessageBus bus;
    private final LeaderboardManager leaderboardManager;
    private final Map<String, Long> lastAnnounced = new ConcurrentHashMap<>();

    @Inject
    public LeaderboardSync(@NotNull Core core, @NotNull MessageBus bus,
                           @NotNull LeaderboardManager leaderboardManager) {
        this.core = core;
        this.bus = bus;
        this.leaderboardManager = leaderboardManager;
        bus.subscribe(TOPIC, this::receive);
    }

    /** Tells the rest of the network that this leaderboard's top ten moved. */
    public void changed(@NotNull Leaderboard<?, ?> leaderboard) {
        if (!bus.isAvailable()) {
            return;
        }

        final String name = leaderboard.getName();
        final long now = System.currentTimeMillis();
        final Long previous = lastAnnounced.get(name);
        if (previous != null && now - previous < ANNOUNCE_INTERVAL_MILLIS) {
            return;
        }

        lastAnnounced.put(name, now);
        bus.publish(BusMessage.of(TOPIC, NAME, name));
    }

    private void receive(@NotNull BusMessage incoming) {
        if (Core.getCurrentRealm().getServer().getName().equals(incoming.getOrigin())) {
            return;
        }

        final Optional<String> name = incoming.get(NAME);
        if (name.isEmpty()) {
            return;
        }

        leaderboardManager.getObject(name.get()).ifPresent(leaderboard ->
                UtilServer.runTaskLaterAsync(core, leaderboard::forceUpdate, REFRESH_DELAY_TICKS));
    }
}
