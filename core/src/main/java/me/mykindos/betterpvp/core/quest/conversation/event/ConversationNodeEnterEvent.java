package me.mykindos.betterpvp.core.quest.conversation.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The conversation arrived at a node and its line has begun.
 * <p>
 * Published so a cutscene can pace its camera against dialogue without either side depending on the other. Conversations
 * announce where they are; anything that cares listens.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ConversationNodeEnterEvent extends CustomEvent {

    private final @NotNull Player player;
    private final @NotNull String conversationId;
    private final @NotNull String nodeId;

    public ConversationNodeEnterEvent(@NotNull Player player, @NotNull String conversationId, @NotNull String nodeId) {
        this.player = player;
        this.conversationId = conversationId;
        this.nodeId = nodeId;
    }
}
