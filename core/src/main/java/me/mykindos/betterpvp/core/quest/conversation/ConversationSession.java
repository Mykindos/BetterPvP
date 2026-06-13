package me.mykindos.betterpvp.core.quest.conversation;

import lombok.Data;
import me.mykindos.betterpvp.core.client.gamer.Gamer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** A player's in-progress conversation state. */
@Data
public class ConversationSession {

    private final UUID playerId;
    private final Gamer gamer;
    private final ConversationDefinition definition;
    private final ConversationActionBar actionBar;
    private String currentNodeId;
    private int selectedIndex = 0;
    private long nodeStartMillis;
    /** How many body characters were visible on the last render, for the typewriter tick sound. */
    private int shownCharCount;

    /**
     * Resolves when the session ends: true if the dialogue ran to a terminal
     * outcome, false if it was aborted (quit / replaced). Always completed by
     * {@link ConversationManager#end}, so awaiting it cannot leak.
     */
    private final CompletableFuture<Boolean> completion = new CompletableFuture<>();
}
