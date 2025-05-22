package me.mykindos.betterpvp.core.chat.channels;

import java.util.Collection;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;

public class StaffChatChannel implements IChatChannel {

    private final ClientManager clientManager;

    public StaffChatChannel(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.STAFF;
    }

    @Override
    public Collection<? extends Player> getAudience() {
        return clientManager.getOnline().stream().filter(client -> client.getGamer().getPlayer() != null
                        && client.getRank().getId() >= Rank.HELPER.getId())
                .map(client -> client.getGamer().getPlayer()).toList();
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.HELPER;
    }

    @Override
    public boolean equals(Object o1) {
        if (this == o1) return true;
        if (o1 == null || getClass() != o1.getClass()) return false;
        StaffChatChannel that = (StaffChatChannel) o1;
        return this.getChannel() == that.getChannel();
    }
}
