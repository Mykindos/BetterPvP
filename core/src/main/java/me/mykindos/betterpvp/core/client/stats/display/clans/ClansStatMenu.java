package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClansStatMenu extends AbstractStatMenu implements IAbstractStatMenu {



    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param periodKey
     * @param statPeriodManager
     */
    protected ClansStatMenu(@NotNull Client client, @Nullable Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(client, previous, periodKey, statPeriodManager);
    }
}
