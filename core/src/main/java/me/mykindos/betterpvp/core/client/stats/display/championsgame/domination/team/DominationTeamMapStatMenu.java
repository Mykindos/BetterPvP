package me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.team;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatPagedMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;

import java.util.List;

public class DominationTeamMapStatMenu extends AbstractStatPagedMenu {

    private final String teamName;

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param period
     */
    public DominationTeamMapStatMenu(Client client, Windowed previous, String periodKey, StatPeriodManager statPeriodManager, String teamName) {
        super(client, previous, periodKey, statPeriodManager);
        this.teamName = teamName;

        final List<String> mapNames = IAbstractStatMenu.getMapNames(getClient(), "Domination");

        final List<Item> items = mapNames.stream()
                .map(mapName -> new DominationTeamMapInfoStatButton(mapName, teamName))
                .map(Item.class::cast)
                .toList();

        setContent(items);
    }
}
