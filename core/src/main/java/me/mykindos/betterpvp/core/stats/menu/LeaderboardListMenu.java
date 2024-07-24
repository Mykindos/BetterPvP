package me.mykindos.betterpvp.core.stats.menu;

import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.inventory.item.impl.SimpleItem;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.impl.ViewCollectionMenu;
import me.mykindos.betterpvp.core.stats.LeaderboardCategory;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

public class LeaderboardListMenu extends ViewCollectionMenu {

    public LeaderboardListMenu(LeaderboardManager manager, LeaderboardCategory category, Windowed previous) {
        super("Leaderboard", new ArrayList<>(), previous);

        setContent(manager.getViewable(category).values().stream()
                .map(lb -> {
                    final ItemView item = ItemView.of(lb.getDescription().getIcon().get()).toBuilder()
                            .action(ClickActions.ALL, Component.text("Open"))
                            .build();

                    return (Item) new SimpleItem(item, click -> new LeaderboardMenu<>(click.getPlayer(), lb, LeaderboardListMenu.this).show(click.getPlayer()));
                })
                .toList());
    }
}
