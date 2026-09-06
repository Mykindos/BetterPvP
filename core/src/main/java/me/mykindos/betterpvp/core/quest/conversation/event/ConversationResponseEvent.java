package me.mykindos.betterpvp.core.quest.conversation.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * A specific response was chosen, fired after its side effects have run and before the conversation branches.
 * <p>
 * The finest-grained rendezvous a cutscene has: a beat that should end on one answer but not another waits on this
 * rather than on the node it was asked from. A response with no authored id reports an empty one, which still
 * identifies the node it belonged to.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ConversationResponseEvent extends CustomEvent {

    private final @NotNull Player player;
    private final @NotNull String conversationId;
    private final @NotNull String nodeId;
    private final @NotNull String responseId;

    public ConversationResponseEvent(@NotNull Player player, @NotNull String conversationId, @NotNull String nodeId,
                                     @NotNull String responseId) {
        this.player = player;
        this.conversationId = conversationId;
        this.nodeId = nodeId;
        this.responseId = responseId;
    }
}
