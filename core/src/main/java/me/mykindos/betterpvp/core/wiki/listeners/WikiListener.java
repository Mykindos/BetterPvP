package me.mykindos.betterpvp.core.wiki.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.weapon.WikiableManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.wiki.menus.event.WikiFetchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class WikiListener implements Listener {

    private final WikiableManager wikiableManager;

    @Inject
    public WikiListener(WikiableManager wikiableManager) {
        this.wikiableManager = wikiableManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMenuOpen(WikiFetchEvent event) {
        wikiableManager.getWikiablesForCategory(event.getCategory()).forEach(event::addWikiable);
    }

}