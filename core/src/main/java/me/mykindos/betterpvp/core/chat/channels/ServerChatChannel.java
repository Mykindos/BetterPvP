package me.mykindos.betterpvp.core.chat.channels;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;

public class ServerChatChannel implements IChatChannel {

    private static ServerChatChannel instance;

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.SERVER;
    }

    @Override
    public Collection<? extends Player> getAudience() {
        return Bukkit.getOnlinePlayers();
    }

    public static ServerChatChannel getInstance() {
        if(instance == null){
            instance = new ServerChatChannel();
        }
        return instance;
    }

}
