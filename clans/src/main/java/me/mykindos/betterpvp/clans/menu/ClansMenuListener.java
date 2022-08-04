package me.mykindos.betterpvp.clans.menu;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.menu.events.MenuOpenEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
public class ClansMenuListener implements Listener {

    private final Clans clans;

    @Inject
    public ClansMenuListener(Clans clans) {
        this.clans = clans;
    }

    @EventHandler
    public void onOpenMenu(MenuOpenEvent event) {
        if (event.getMenu() instanceof InjectableMenu injectableMenu) {
            clans.getInjector().injectMembers(event.getMenu());
            injectableMenu.postInjection();
        }
    }
}