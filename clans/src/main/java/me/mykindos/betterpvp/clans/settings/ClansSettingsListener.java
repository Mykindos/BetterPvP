package me.mykindos.betterpvp.clans.settings;

import me.mykindos.betterpvp.clans.settings.menus.ClansSettingsMenu;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
public class ClansSettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(SettingsFetchEvent event) {
        event.supply(new ClansSettingsMenu(event.getGamer()));
    }


}
