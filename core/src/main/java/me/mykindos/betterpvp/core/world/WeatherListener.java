package me.mykindos.betterpvp.core.world;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

@BPvPListener
@Singleton
public class WeatherListener implements Listener {

    @Inject
    @Config(path = "weather.enabled", defaultValue = "false")
    private boolean weatherEnabled;

    @EventHandler
    public void onWeatherChangeEvent(WeatherChangeEvent event) {
        if((!weatherEnabled) && event.toWeatherState()) {
            event.setCancelled(true);
        }
    }
}
