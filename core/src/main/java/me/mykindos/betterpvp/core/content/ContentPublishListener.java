package me.mykindos.betterpvp.core.content;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.purity.bias.PurityReforgeBiasRegistry;
import me.mykindos.betterpvp.core.item.purity.distribution.PurityDistributionRegistry;
import me.mykindos.betterpvp.core.item.runeslot.RuneSlotDistributionRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.quest.QuestRegistry;
import me.mykindos.betterpvp.core.quest.cinematic.CinematicRegistry;
import me.mykindos.betterpvp.core.quest.conversation.ConversationRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.event.Listener;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listens on Postgres NOTIFY channels so the game hot-reloads the moment the
 * admin console changes data — no restart, no polling:
 * <ul>
 *   <li>{@code content_published} → reload loot / quest / conversation / cinematic</li>
 *   <li>{@code tuning_changed} → reload purity / reforge-bias / rune-slot config</li>
 * </ul>
 * The DB is the single source of truth; these are just the change signals.
 */
@Singleton
@BPvPListener
@CustomLog
public class ContentPublishListener implements Listener {

    private static final String CONTENT_CHANNEL = "content_published";
    private static final String TUNING_CHANNEL = "tuning_changed";

    private final Core core;
    private final Database database;
    private final LootTableRegistry lootTableRegistry;
    private final QuestRegistry questRegistry;
    private final ConversationRegistry conversationRegistry;
    private final CinematicRegistry cinematicRegistry;
    private final PurityDistributionRegistry purityDistributionRegistry;
    private final PurityReforgeBiasRegistry purityReforgeBiasRegistry;
    private final RuneSlotDistributionRegistry runeSlotDistributionRegistry;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean running = true;

    @Inject
    public ContentPublishListener(Core core, Database database, LootTableRegistry lootTableRegistry,
                                  QuestRegistry questRegistry, ConversationRegistry conversationRegistry,
                                  CinematicRegistry cinematicRegistry, PurityDistributionRegistry purityDistributionRegistry,
                                  PurityReforgeBiasRegistry purityReforgeBiasRegistry,
                                  RuneSlotDistributionRegistry runeSlotDistributionRegistry) {
        this.core = core;
        this.database = database;
        this.lootTableRegistry = lootTableRegistry;
        this.questRegistry = questRegistry;
        this.conversationRegistry = conversationRegistry;
        this.cinematicRegistry = cinematicRegistry;
        this.purityDistributionRegistry = purityDistributionRegistry;
        this.purityReforgeBiasRegistry = purityReforgeBiasRegistry;
        this.runeSlotDistributionRegistry = runeSlotDistributionRegistry;
    }

    @UpdateEvent(delay = 10000)
    public void startOnce() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(this::listenLoop, "bpvp-db-listen");
        thread.setDaemon(true);
        thread.start();
    }

    private void listenLoop() {
        final DataSource dataSource = database.getConnection().getDataSource();
        while (running) {
            try (Connection connection = dataSource.getConnection()) {
                final PGConnection pgConnection = connection.unwrap(PGConnection.class);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LISTEN " + CONTENT_CHANNEL);
                    statement.execute("LISTEN " + TUNING_CHANNEL);
                }
                while (running && !connection.isClosed()) {
                    PGNotification[] notifications = pgConnection.getNotifications(5000);
                    if (notifications == null || notifications.length == 0) continue;

                    boolean content = false;
                    boolean tuning = false;
                    for (PGNotification notification : notifications) {
                        if (TUNING_CHANNEL.equals(notification.getName())) tuning = true;
                        else content = true;
                    }
                    if (content) onContentPublished();
                    if (tuning) onTuningChanged();
                }
            } catch (Exception ex) {
                log.warn("DB notify listener dropped; reconnecting in 5s", ex).submit();
                sleep();
            }
        }
    }

    private void onContentPublished() {
        // Reload on the main thread (loot reload touches Bukkit).
        UtilServer.runTask(core, () -> {
            lootTableRegistry.reload();
            questRegistry.reload();
            conversationRegistry.reload();
            cinematicRegistry.reload();
        });
    }

    private void onTuningChanged() {
        UtilServer.runTask(core, () -> {
            purityDistributionRegistry.reload();
            purityReforgeBiasRegistry.reload();
            runeSlotDistributionRegistry.reload();
        });
    }

    private void sleep() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            running = false;
        }
    }
}
