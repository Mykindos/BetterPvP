package me.mykindos.betterpvp.core.chat.channels.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import org.jetbrains.annotations.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
public class PlayerChangeChatChannelEvent extends CustomCancellableEvent {

    private final Gamer gamer;
    private final ChatChannel targetChannel;
    @Nullable public IChatChannel newChannel;

}
