package me.mykindos.betterpvp.core.chat.channels;

import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface IChatChannel {

    ChatChannel getChannel();

    /**
     * Everyone this channel delivers to when {@code sender} speaks. The sender matters because the local channel
     * reaches whoever is around them, while the rest have a roster of their own.
     */
    Collection<? extends Player> getAudience(Player sender);

    default Rank getRequiredRank() {
        return Rank.PLAYER;
    }

}
