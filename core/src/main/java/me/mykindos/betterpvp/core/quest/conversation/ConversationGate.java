package me.mykindos.betterpvp.core.quest.conversation;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides whether a node is still waiting on something outside the conversation before it may speak.
 * <p>
 * A node's {@code await} key is opaque here on purpose. Conversations must not know what a cutscene is - that
 * dependency runs one way, and this interface is what keeps it that way: the conversation asks "is this key still
 * holding?" and whoever registered a gate answers. Nothing else about the key is interpreted.
 * <p>
 * A held node draws its speaker and its empty box but does not start typing and accepts no input, and its typewriter
 * begins from the moment the hold lifts - so a line paced against a four-second camera move starts exactly as the
 * shot lands rather than halfway through it.
 *
 * @see ConversationManager#setGate(ConversationGate)
 */
@FunctionalInterface
public interface ConversationGate {

    /**
     * @param key the node's {@code await} value, meaningful only to whoever registered this gate
     * @return true while the node should keep waiting. An unrecognised key must return false, so a conversation is
     * never deadlocked by a hold nobody is able to release.
     */
    boolean isHeld(@NotNull Player player, @NotNull String key);
}
