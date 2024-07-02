package me.mykindos.betterpvp.core.wiki.menus.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.wiki.types.IWikiable;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;

@Getter
@RequiredArgsConstructor
public class WikiFetchEvent extends CustomEvent {
    private final WikiCategory category;
    private final Player player;
    private final Client client;

    private final ArrayList<IWikiable> wikiables = new ArrayList<>();

    public void addWikiable(IWikiable newWikiable) {
        wikiables.add(newWikiable);
    }
}
