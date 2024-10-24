package me.mykindos.betterpvp.core.settings.listeners;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.menus.GeneralSettingsMenu;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class SettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(SettingsFetchEvent event) {
        event.supply(new GeneralSettingsMenu(event.getPlayer(), event.getClient(), event.getSettingsMenu()));
    }

}
