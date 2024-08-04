package me.mykindos.betterpvp.core.wiki.menus;

import lombok.NonNull;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.wiki.menus.buttons.WikiCategoryButton;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class WikiMenu extends ViewCollectionMenu implements Windowed {

    public WikiMenu(@NonNull Player player, @NonNull Client client) {
        super("Wiki",
                Arrays.stream(WikiCategory.values())
                        .filter(wikiCategory -> wikiCategory.getParent() == null)
                        .map(wikiCategory -> new WikiCategoryButton(player, client, wikiCategory, null))
                        .collect(Collectors.toList()),
                null);
        //update the parent
        this.content.forEach(object -> {
            if (object instanceof WikiCategoryButton wikiCategoryButton) {
                wikiCategoryButton.setParent(this);
            }
        });
    }
}
