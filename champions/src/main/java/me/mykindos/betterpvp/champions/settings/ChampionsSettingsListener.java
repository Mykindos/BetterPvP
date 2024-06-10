package me.mykindos.betterpvp.champions.settings;

import me.mykindos.betterpvp.champions.settings.menus.ChampionsSettingsMenu;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
public class ChampionsSettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(SettingsFetchEvent event) {
        event.supply(new ChampionsSettingsMenu(event.getClient()));
    }


}
