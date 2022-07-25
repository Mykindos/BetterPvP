package me.mykindos.betterpvp.clans.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.framework.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

@BPvPListener
public class DemoListener implements Listener {

    @Inject
    public Clans clans;

    @UpdateEvent(delay = 250)
    public void onUpdate(){
        clans.getLogger().info("UpdateEvent triggered");
    }
}
