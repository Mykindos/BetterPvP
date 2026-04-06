package me.mykindos.betterpvp.hub.commands.queue;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.orchestration.model.QueueSnapshot;
import me.mykindos.betterpvp.orchestration.model.QueueStatusUpdate;
import me.mykindos.betterpvp.orchestration.model.QueueTarget;
import me.mykindos.betterpvp.orchestration.model.QueueTargetType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletionException;

final class QueueCommandSupport {

    private QueueCommandSupport() {
    }

    static Component buildQueueMessage(QueueStatusUpdate status) {
        Component message = Component.text()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Server: ", NamedTextColor.GRAY))
                .append(Component.text(status.queuedTarget().serverName(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Position: ", NamedTextColor.GRAY))
                .append(Component.text("#" + status.position(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Queue Size: ", NamedTextColor.GRAY))
                .append(Component.text(status.queueSize(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("State: ", NamedTextColor.GRAY))
                .append(Component.text(status.state().name(), NamedTextColor.YELLOW))
                .build();

        if (status.estimatedWaitSeconds() != null) {
            message = message.appendNewline()
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("Estimated Wait: ", NamedTextColor.GRAY))
                    .append(Component.text(status.estimatedWaitSeconds() + "s", NamedTextColor.YELLOW));
        }

        return message;
    }

    static Component buildUsageMessage(Client client) {
        Component message = Component.text()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("/queue", NamedTextColor.YELLOW))
                .append(Component.text(" - View your current queue status.", NamedTextColor.GRAY))
                .build();

        if (client.getRank().getId() >= Rank.ADMIN.getId()) {
            message = message.appendNewline()
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/queue view <server>", NamedTextColor.YELLOW))
                    .append(Component.text(" - View queue details for a server.", NamedTextColor.GRAY))
                    .appendNewline()
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/queue pause <server>", NamedTextColor.YELLOW))
                    .append(Component.text(" - Pause new queue admissions for a server.", NamedTextColor.GRAY))
                    .appendNewline()
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/queue resume <server>", NamedTextColor.YELLOW))
                    .append(Component.text(" - Re-open queue admissions for a server.", NamedTextColor.GRAY))
                    .appendNewline()
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/queue remove <player>", NamedTextColor.YELLOW))
                    .append(Component.text(" - Remove a player from the queue.", NamedTextColor.GRAY))
                    .appendNewline()
                    .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                    .append(Component.text("/queue admit <player>", NamedTextColor.YELLOW))
                    .append(Component.text(" - Force the next admission for a queued player.", NamedTextColor.GRAY));
        }

        return message;
    }

    static Component buildQueueSnapshotMessage(QueueSnapshot snapshot) {
        return Component.text()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Server: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.target().serverName(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("State: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.state().name(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Queued Players: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.queueSize(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Soft Capacity: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.softCapacity(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Regular Online: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.regularOnline(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Bypass Online: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.bypassOnline(), NamedTextColor.YELLOW))
                .appendNewline()
                .append(Component.text("● ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Reserved Slots: ", NamedTextColor.GRAY))
                .append(Component.text(snapshot.reservedRegularSlots(), NamedTextColor.YELLOW))
                .build();
    }

    static QueueTarget buildClansTarget(String serverName) {
        return new QueueTarget("clans:" + serverName.toLowerCase(), QueueTargetType.CLANS, serverName);
    }

    static void logCommandFailure(String action, Exception ex) {
        final Throwable root = unwrap(ex);
        Bukkit.getLogger().warning("[Queue] Failed to " + action + ": " + root.getClass().getSimpleName()
                + (root.getMessage() == null ? "" : ": " + root.getMessage()));
        root.printStackTrace();
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof RuntimeException)
                && current.getCause() != null
                && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
