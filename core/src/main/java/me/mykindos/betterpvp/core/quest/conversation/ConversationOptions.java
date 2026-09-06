package me.mykindos.betterpvp.core.quest.conversation;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

/**
 * Which parts of a conversation's presentation it owns, and which its caller has already taken care of.
 * <p>
 * A conversation normally arranges everything it needs: it freezes the player, claims the action bar, and draws its own
 * darkening backdrop. Inside a cutscene all three are wrong. The player is already in spectator, so the freeze is a
 * second gate on top of one that already holds - and its movement-speed modifier visibly changes the field of view,
 * which is the last thing a framed shot wants. The action bar and the backdrop belong to the letterbox, which
 * composites the dialogue into itself rather than fighting it for the same surface.
 * <p>
 * Expressed as options handed <em>in</em> rather than as the conversation asking whether a cutscene is running, so
 * conversations keep knowing nothing about cutscenes and the dependency runs one way only.
 */
@Value
public class ConversationOptions {

    private static final ConversationOptions STANDALONE = new ConversationOptions(true, true, true);
    private static final ConversationOptions MANAGED = new ConversationOptions(false, false, false);

    /** Whether to hold the player still for the duration. */
    boolean freeze;

    /** Whether to claim the action bar and render itself onto it. */
    boolean ownActionBar;

    /** Whether to draw the fog behind the dialogue box. */
    boolean drawBackdrop;

    /** An ordinary conversation, responsible for its own presentation. */
    public static @NotNull ConversationOptions standalone() {
        return STANDALONE;
    }

    /**
     * A conversation running inside something that has already gated the player and owns the screen - a cutscene. It
     * still branches, gates responses and completes exactly as any other; only the presentation is somebody else's.
     */
    public static @NotNull ConversationOptions managedByCaller() {
        return MANAGED;
    }
}
