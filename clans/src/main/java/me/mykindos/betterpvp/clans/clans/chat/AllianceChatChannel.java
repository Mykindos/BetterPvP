package me.mykindos.betterpvp.clans.clans.chat;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class AllianceChatChannel implements IChatChannel {

    private final Clan clan;

    public AllianceChatChannel(Clan clan) {
        this.clan = clan;
    }

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.ALLIANCE;
    }

    @Override
    public Collection<? extends Player> getAudience(Player sender) {
        List<Player> players = clan.getMembersAsPlayers();
        clan.getAlliances().forEach(alliance -> players.addAll(alliance.getClan().getMembersAsPlayers()));
        return players;
    }

    @Override
    public @NotNull Optional<String> getNetworkKey(@NotNull Player sender) {
        return Optional.of(String.valueOf(clan.getId()));
    }

}
