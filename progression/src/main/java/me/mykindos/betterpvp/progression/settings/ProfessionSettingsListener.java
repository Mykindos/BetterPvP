package me.mykindos.betterpvp.progression.settings;

import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.menus.event.SettingsFetchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
public class ProfessionSettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(SettingsFetchEvent event) {
        event.supply(new ProfessionSettingsMenu(event.getClient(), event.getSettingsMenu()));
    }


}
