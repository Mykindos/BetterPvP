package me.mykindos.betterpvp.hub.feature.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.display.DisplayObject;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@BPvPListener
@Singleton
public class HubQueueDisplayListener implements Listener {

    private static final int ACTION_BAR_PRIORITY = 75;
    private static final Duration POSITION_REMINDER_INTERVAL = Duration.ofSeconds(30);

    private final HubQueueStatusRegistry queueStatusRegistry;
    private final ClientManager clientManager;
    private final Map<UUID, DisplayObject<Component>> actionBarComponents = new ConcurrentHashMap<>();
    private final Map<UUID, QueueStatusSnapshot> lastSentSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastSentTimes = new ConcurrentHashMap<>();

    @Inject
    public HubQueueDisplayListener(HubQueueStatusRegistry queueStatusRegistry, ClientManager clientManager) {
        this.queueStatusRegistry = queueStatusRegistry;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onJoin(ClientJoinEvent event) {
        attachActionBarComponent(event.getClient());
    }

    @EventHandler
    public void onQuit(ClientQuitEvent event) {
        final UUID uuid = event.getPlayer().getUniqueId();
        final DisplayObject<Component> component = actionBarComponents.remove(uuid);
        if (component != null) {
            event.getClient().getGamer().getActionBar().remove(component);
        }
        lastSentSnapshots.remove(uuid);
        lastSentTimes.remove(uuid);
        queueStatusRegistry.clear(uuid);
    }

    @UpdateEvent(delay = 250L)
    public void attachMissingActionBarComponents() {
        clientManager.getOnline().forEach(this::attachActionBarComponent);
    }

    @UpdateEvent(delay = 250L)
    public void notifyQueuedPlayers() {
        final Instant now = Instant.now();
        clientManager.getOnline().forEach(client -> {
            final UUID uuid = client.getUniqueId();
            final QueueStatusUpdate status = queueStatusRegistry.getStatus(uuid).orElse(null);
            if (status == null) {
                lastSentSnapshots.remove(uuid);
                lastSentTimes.remove(uuid);
                return;
            }

            final QueueStatusSnapshot snapshot = new QueueStatusSnapshot(
                    status.queuedTarget().targetId(),
                    status.position(),
                    status.queueSize(),
                    status.readyToConnect()
            );
            final QueueStatusSnapshot previous = lastSentSnapshots.get(uuid);
            final Instant previousSentTime = lastSentTimes.get(uuid);
            final boolean changed = !snapshot.equals(previous);
            final boolean reminderDue = previousSentTime == null || previousSentTime.plus(POSITION_REMINDER_INTERVAL).isBefore(now);
            if (!changed && !reminderDue) {
                return;
            }

            sendQueuePosition(client, status, previous == null);
            lastSentSnapshots.put(uuid, snapshot);
            lastSentTimes.put(uuid, now);
        });
    }

    private void attachActionBarComponent(Client client) {
        final UUID uuid = client.getUniqueId();
        actionBarComponents.computeIfAbsent(uuid, ignored -> {
            final DisplayObject<Component> component = new DisplayObject<>(gamer ->
                    queueStatusRegistry.getStatus(uuid)
                            .map(this::buildActionBar)
                            .orElse(null));
            client.getGamer().getActionBar().add(ACTION_BAR_PRIORITY, component);
            return component;
        });
    }

    private Component buildActionBar(QueueStatusUpdate update) {
        final Component prefix = Component.text("Queue ", NamedTextColor.GOLD);
        return prefix.append(Component.text("#" + update.position(), NamedTextColor.YELLOW))
                .append(Component.text(" for ", NamedTextColor.GRAY))
                .append(Component.text(update.queuedTarget().serverName(), NamedTextColor.YELLOW))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(formatElapsed(update.enqueuedAt()), NamedTextColor.GOLD));
    }

    private void sendQueuePosition(Client client, QueueStatusUpdate status, boolean firstSeen) {
        if (firstSeen) {
            UtilMessage.simpleMessage(client.getGamer().getPlayer(), "Queue",
                    "<gray>You were added to the queue for <yellow>" + status.queuedTarget().serverName()
                            + "<gray> at position <yellow>#"
                            + Math.max(1, status.position()) + "<gray>.");
            return;
        }

        UtilMessage.simpleMessage(client.getGamer().getPlayer(), "Queue",
                "<gray>Your queue position for <yellow>" + status.queuedTarget().serverName()
                        + "<gray> is <yellow>#"
                        + status.position()
                        + "<gray> of <yellow>"
                        + status.queueSize());
    }

    private String formatElapsed(Instant enqueuedAt) {
        final long totalSeconds = Math.max(0L, Duration.between(enqueuedAt, Instant.now()).getSeconds());
        final long minutes = totalSeconds / 60L;
        final long seconds = totalSeconds % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    private record QueueStatusSnapshot(String targetId, int position, int queueSize, boolean readyToConnect) {
    }
}
