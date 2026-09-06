package me.mykindos.betterpvp.core.cutscene.signal;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.cutscene.CutsceneManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationEndEvent;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationNodeEnterEvent;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationNodeExitEvent;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationResponseEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Turns what a conversation announces into signals the watching cutscene can wait on.
 * <p>
 * This one class is the entire dependency between the two tracks, and it points one way: conversations publish where
 * they are and know nothing about cutscenes; cutscenes listen. That is what lets a camera beat be paced against a
 * conversation somebody authored in the admin console this morning, with no console change and no code change at
 * either end - the signal names are derived from node ids that already exist.
 */
@Singleton
@BPvPListener
public class CutsceneSignalListener implements Listener {

    private final CutsceneManager manager;

    @Inject
    public CutsceneSignalListener(CutsceneManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNodeEnter(ConversationNodeEnterEvent event) {
        emit(event.getPlayer(), Signal.node(event.getNodeId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNodeExit(ConversationNodeExitEvent event) {
        emit(event.getPlayer(), Signal.nodeEnd(event.getNodeId()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onResponse(ConversationResponseEvent event) {
        if (!event.getResponseId().isBlank()) {
            emit(event.getPlayer(), Signal.response(event.getNodeId(), event.getResponseId()));
        }
    }

    /**
     * A dialogue track that finishes before the camera track does announces it, so a beat waiting on a node that will
     * now never be reached has something to fall back on.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEnd(ConversationEndEvent event) {
        emit(event.getPlayer(), Signal.conversationEnd());
    }

    private void emit(@NotNull Player player, @NotNull Signal signal) {
        manager.session(player).ifPresent(session -> session.emit(signal));
    }
}
