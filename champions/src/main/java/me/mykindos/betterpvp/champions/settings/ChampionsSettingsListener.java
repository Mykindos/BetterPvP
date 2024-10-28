package me.mykindos.betterpvp.champions.settings;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ChampionsSettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(SettingsFetchEvent event) {
        event.supply(new ChampionsSettingsMenu(event.getClient(), event.getSettingsMenu()));
    }


}
