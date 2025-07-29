package me.mykindos.betterpvp.game.framework.model.chat;

import java.util.Collection;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import me.mykindos.betterpvp.game.framework.model.player.PlayerController;
import org.bukkit.entity.Player;

public class SpectatorChatChannel implements IChatChannel {
    private final PlayerController playerController;

    public SpectatorChatChannel(PlayerController playerController) {
        this.playerController = playerController;
    }

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.SPECTATOR;
    }

    @Override
    public Collection<? extends Player> getAudience() {
        return playerController.getSpectators().keySet();
    }
}
