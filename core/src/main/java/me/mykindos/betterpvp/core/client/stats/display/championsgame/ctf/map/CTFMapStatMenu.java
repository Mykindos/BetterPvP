package me.mykindos.betterpvp.core.client.stats.display.championsgame.ctf.map;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatPagedMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;

import java.util.List;

public class CTFMapStatMenu extends AbstractStatPagedMenu {

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param period
     */
    public CTFMapStatMenu(Client client, Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(client, previous, periodKey, statPeriodManager);

        final List<String> mapNames = IAbstractStatMenu.getMapNames(getClient(), "Capture The Flag");

        final List<Item> items = mapNames.stream()
                .map(CTFMapInfoStatButton::new)
                .map(Item.class::cast)
                .toList();

        setContent(items);
    }
}
