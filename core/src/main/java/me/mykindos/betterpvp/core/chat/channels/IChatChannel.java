package me.mykindos.betterpvp.core.chat.channels;

import me.mykindos.betterpvp.core.client.Rank;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public interface IChatChannel {

    ChatChannel getChannel();

    /**
     * Everyone this channel delivers to when {@code sender} speaks. The sender matters because the local channel
     * reaches whoever is around them, while the rest have a roster of their own.
     *
     * @param sender null when the message came from another server, which only a channel with a roster can be asked
     */
    Collection<? extends Player> getAudience(@Nullable Player sender);

    /**
     * What names this channel on another server, or empty when it does not leave this one.
     * <p>
     * A key has to be enough for the receiving server to work out its own audience without the sender being there,
     * so it identifies the roster rather than the people in it. A clan channel is named by the clan id, and staff
     * chat by nothing at all because every server already knows who its staff are.
     */
    default @NotNull Optional<String> getNetworkKey(@NotNull Player sender) {
        return Optional.empty();
    }

    default Rank getRequiredRank() {
        return Rank.PLAYER;
    }

}
