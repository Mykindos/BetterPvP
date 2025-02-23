package me.mykindos.betterpvp.clans.clans.chat;

import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ClanChatChannel implements IChatChannel {

    private final Clan clan;

    public ClanChatChannel(@NotNull Clan clan) {
        this.clan = clan;
    }

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.CLAN;
    }

    @Override
    public Collection<? extends Player> getAudience() {
        return clan.getMembersAsPlayers();
    }
}
