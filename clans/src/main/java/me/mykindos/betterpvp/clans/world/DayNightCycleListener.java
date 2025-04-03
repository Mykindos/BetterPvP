package me.mykindos.betterpvp.clans.world;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

@BPvPListener
public class DayNightCycleListener implements Listener {

    @Inject
    @Config(path = "server.world.mainWorld", defaultValue = BPvPWorld.MAIN_WORLD_NAME)
    private String mainWorld;

    @Inject
    @Config(path = "server.world.fastNightCycle", defaultValue = "true")
    private boolean fastNight;

    @UpdateEvent(delay = 100)
    public void onTimeUpdate() {
        if (fastNight) {
            World world = Bukkit.getWorld(mainWorld);
            if (world != null) {
                if (world.getTime() > 13000) {
                    world.setTime(world.getTime() + 20);
                }
            }
        }
    }
}
