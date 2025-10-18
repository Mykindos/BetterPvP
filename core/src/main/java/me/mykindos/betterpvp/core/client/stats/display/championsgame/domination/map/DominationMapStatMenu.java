package me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.map;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatPagedMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;

import java.util.List;

public class DominationMapStatMenu extends AbstractStatPagedMenu {

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param period
     */
    public DominationMapStatMenu(Client client, Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(client, previous, periodKey, statPeriodManager);

        final List<String> mapNames = IAbstractStatMenu.getMapNames(getClient(), "Domination");

        final List<Item> items = mapNames.stream()
                .map(DominationMapInfoStatButton::new)
                .map(Item.class::cast)
                .toList();

        setContent(items);
    }
}
