package me.mykindos.betterpvp.core.quest.conversation.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * The conversation left a node, which happens when one of its responses is confirmed.
 * <p>
 * Published so a cutscene can pace its camera against dialogue without either side depending on the other. Conversations
 * announce where they are; anything that cares listens.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class ConversationNodeExitEvent extends CustomEvent {

    private final @NotNull Player player;
    private final @NotNull String conversationId;
    private final @NotNull String nodeId;

    public ConversationNodeExitEvent(@NotNull Player player, @NotNull String conversationId, @NotNull String nodeId) {
        this.player = player;
        this.conversationId = conversationId;
        this.nodeId = nodeId;
    }
}
