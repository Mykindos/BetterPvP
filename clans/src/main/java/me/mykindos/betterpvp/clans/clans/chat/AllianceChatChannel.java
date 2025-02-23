package me.mykindos.betterpvp.clans.clans.chat;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;

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
    public Collection<? extends Player> getAudience() {
        List<Player> players = clan.getMembersAsPlayers();
        clan.getAlliances().forEach(alliance -> players.addAll(alliance.getClan().getMembersAsPlayers()));
        return players;
    }

}
