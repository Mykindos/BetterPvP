package me.mykindos.betterpvp.core.settings.listeners;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.events.MenuOpenEvent;
import me.mykindos.betterpvp.core.settings.menus.SettingsMenu;
import me.mykindos.betterpvp.core.settings.menus.buttons.GeneralCategoryButton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class SettingsListener implements Listener {

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(MenuOpenEvent event){
        if(!(event.getMenu() instanceof SettingsMenu menu)) return;

        menu.addButton(new GeneralCategoryButton());

    }

}
