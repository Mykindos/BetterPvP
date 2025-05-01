package me.mykindos.betterpvp.clans;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.mineplex.events.MineplexMessageReceivedEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ChampionsClansRewardListener implements Listener {

    @EventHandler
    public void onChampionsClansReward(MineplexMessageReceivedEvent event) {
        Bukkit.broadcastMessage(event.getChannel() + " " + event.getMessage().getMessage() + " " + event.getMessage().getMetadata().get("uuid"));
    }
}
