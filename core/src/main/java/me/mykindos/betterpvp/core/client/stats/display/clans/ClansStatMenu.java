package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.filter.ClanContext;
import me.mykindos.betterpvp.core.client.stats.display.start.ClansStatButton;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.menu.Windowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClansStatMenu extends AbstractClansStatMenu {

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param periodKey
     * @param statPeriodManager
     * @param clanFilterButton
     */
    public ClansStatMenu(@NotNull Client client, @Nullable Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(client, previous, periodKey, ClanContext.ALL, statPeriodManager);
        setItem(1, 2, new PersonalClansStatButton());
        setItem(2, 2, new PillageClansStatButton());
        setItem(4, 2, new ClansStatButton<IAbstractClansStatMenu>());
        setItem(6, 2, new EventClansStatButton());
        setItem(7, 2, new DungeonClansStatButton());
    }
}
