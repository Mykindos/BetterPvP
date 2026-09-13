package me.mykindos.betterpvp.core.chat.channels;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.world.site.Presence;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

/**
 * The channel a player speaks on by default, heard by whoever is present with them.
 */
public class ServerChatChannel implements IChatChannel {

    private static ServerChatChannel instance;

    @Override
    public ChatChannel getChannel() {
        return ChatChannel.SERVER;
    }

    @Override
    public Collection<? extends Player> getAudience(Player sender) {
        return JavaPlugin.getPlugin(Core.class).getInjector().getInstance(Presence.class).around(sender);
    }

    public static ServerChatChannel getInstance() {
        if(instance == null){
            instance = new ServerChatChannel();
        }
        return instance;
    }

}
