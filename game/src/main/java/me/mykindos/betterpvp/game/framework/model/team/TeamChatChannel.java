package me.mykindos.betterpvp.game.framework.model.team;

import java.util.Collection;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.channels.IChatChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeamChatChannel implements IChatChannel {
    @NotNull
    private final Team team;

    public TeamChatChannel(@NotNull Team team) {
        this.team = team;
    }

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.TEAM;
    }

    @Override
    public Collection<? extends Player> getAudience() {
        return team.getPlayers();
    }
}
