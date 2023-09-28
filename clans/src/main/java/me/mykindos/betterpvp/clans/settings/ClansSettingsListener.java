package me.mykindos.betterpvp.clans.settings;

import me.mykindos.betterpvp.clans.settings.buttons.ClansCategoryButton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.events.MenuOpenEvent;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@BPvPListener
public class ClansSettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(MenuOpenEvent event){
        if(!(event.getMenu() instanceof SettingsMenu menu)) return;

        menu.addButton(new ClansCategoryButton());

    }


}
