package me.mykindos.betterpvp.core.quest.conversation.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A conversation finished, whether it ran to a terminal outcome or was aborted.
 * <p>
 * A cutscene whose dialogue track ends before its camera track does needs to know, so it can stop waiting on signals
 * that are never coming - the deadlock the whole rendezvous design exists to avoid.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ConversationEndEvent extends CustomEvent {

    private final @NotNull Player player;
    private final @NotNull String conversationId;

    /** True if the dialogue reached a terminal outcome rather than being cut short. */
    private final boolean completed;

    public ConversationEndEvent(@NotNull Player player, @NotNull String conversationId, boolean completed) {
        this.player = player;
        this.conversationId = conversationId;
        this.completed = completed;
    }
}
