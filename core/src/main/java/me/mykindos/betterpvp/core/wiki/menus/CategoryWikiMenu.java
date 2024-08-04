package me.mykindos.betterpvp.core.wiki.menus;

import lombok.NonNull;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.wiki.menus.buttons.WikiCategoryButton;
import me.mykindos.betterpvp.core.wiki.types.WikiCategory;
import org.bukkit.entity.Player;
import xyz.xenondevs.invui.item.Item;

import java.util.List;

public class CategoryWikiMenu extends ViewCollectionMenu implements Windowed {

    public CategoryWikiMenu(@NonNull Player player, @NonNull Client client, WikiCategory category, List<Item> items, Windowed parent) {
        super("Wiki", items, parent);
        this.content.forEach(object -> {
            if (object instanceof WikiCategoryButton wikiCategoryButton) {
                wikiCategoryButton.setParent(this);
            }
        });
    }
}
