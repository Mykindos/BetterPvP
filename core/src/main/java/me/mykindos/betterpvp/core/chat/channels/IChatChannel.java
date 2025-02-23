package me.mykindos.betterpvp.core.chat.channels;

import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface IChatChannel {

    ChatChannel getChannel();

    Collection<? extends Player> getAudience();

    default Rank getRequiredRank() {
        return Rank.PLAYER;
    }

}
