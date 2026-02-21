package me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.map;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatPagedMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.server.Period;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DominationMapStatMenu extends AbstractStatPagedMenu {

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param period
     */
    public DominationMapStatMenu(@NotNull Client client, @Nullable Windowed previous, StatFilterType type, Period period, RealmManager realmManager) {
        super(client, previous, type, period, realmManager);

        final List<String> mapNames = IAbstractStatMenu.getMapNames(getClient(), "Domination");

        final List<Item> items = mapNames.stream()
                .map(DominationMapInfoStatButton::new)
                .map(Item.class::cast)
                .toList();

        setContent(items);
    }
}
