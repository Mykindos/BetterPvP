package me.mykindos.betterpvp.core.client.stats.display.championsgame.ctf;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.menu.Windowed;

public class CTFStatMenu extends AbstractStatMenu {

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param period
     */
    public CTFStatMenu(Client client, Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(client, previous, periodKey, statPeriodManager);
        setItem(2,2, new CTFTeamStatButton("Blue"));
        setItem(4, 2, new CTFTeamStatButton("Red"));
        setItem(6,2, new CTFMapStatButton());
    }
}
