package me.mykindos.betterpvp.core.chat.channels;

import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

public class StaffChatChannel implements IChatChannel {

    /** Carried so the message names a channel, and ignored on arrival since staff are found without it. */
    public static final String KEY = "staff";

    private final ClientManager clientManager;

    public StaffChatChannel(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.STAFF;
    }

    @Override
    public Collection<? extends Player> getAudience(@Nullable Player sender) {
        return clientManager.getOnline().stream().filter(client -> client.getGamer().getPlayer() != null
                        && client.getRank().getId() >= Rank.TRIAL_MOD.getId())
                .map(client -> client.getGamer().getPlayer()).toList();
    }

    @Override
    public @NotNull Optional<String> getNetworkKey(@NotNull Player sender) {
        return Optional.of(KEY);
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.TRIAL_MOD;
    }

    @Override
    public boolean equals(Object o1) {
        if (this == o1) return true;
        if (o1 == null || getClass() != o1.getClass()) return false;
        StaffChatChannel that = (StaffChatChannel) o1;
        return this.getChannel() == that.getChannel();
    }
}
