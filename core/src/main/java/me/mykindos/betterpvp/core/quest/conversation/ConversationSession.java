package me.mykindos.betterpvp.core.quest.conversation;

import lombok.Data;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** A player's in-progress conversation state. */
@Data
public class ConversationSession {

    private final UUID playerId;
    private final Gamer gamer;
    private final ConversationDefinition definition;
    /** Null when the caller owns the screen and renders this conversation itself - see {@link ConversationOptions}. */
    private @Nullable ConversationActionBar actionBar;
    /** Not final: a response can grow a camera track, at which point the cutscene takes over the presentation. */
    @Setter
    private ConversationOptions options;

    public ConversationSession(UUID playerId, Gamer gamer, ConversationDefinition definition,
                               @Nullable ConversationActionBar actionBar, ConversationOptions options) {
        this.playerId = playerId;
        this.gamer = gamer;
        this.definition = definition;
        this.actionBar = actionBar;
        this.options = options;
    }
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
