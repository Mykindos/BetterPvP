package me.mykindos.betterpvp.core.client.stats.display.championsgame.domination;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.server.Period;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DominationStatMenu extends AbstractStatMenu {

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param periodKey
     */
    public DominationStatMenu(@NotNull Client client, @Nullable Windowed previous, StatFilterType type, Period period, RealmManager realmManager) {
        super(client, previous, type, period, realmManager);
        setItem(2,2, new DominationTeamStatButton("Blue"));
        setItem(4, 2, new DominationTeamStatButton("Red"));
        setItem(6,2, new DominationMapStatButton());
    }
}
