package me.mykindos.betterpvp.core.client.stats.display.clans;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.filter.ClanContext;
import me.mykindos.betterpvp.core.client.stats.display.filter.ClanFilterButton;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.filter.IContextFilterButton;
import me.mykindos.betterpvp.core.server.Period;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public abstract class AbstractClansStatMenu extends AbstractStatMenu implements IAbstractClansStatMenu {

    private final IContextFilterButton<ClanContext> clanFilterButton;
    private ClanContext clanContext;

    /**
     * Creates a new {@link AbstractGui} with the specified width and height.
     *
     * @param client
     * @param previous
     * @param periodKey
     * @param statPeriodManager
     */
    protected AbstractClansStatMenu(@NotNull Client client, @Nullable Windowed previous, StatFilterType type, @Nullable Period period, ClanContext clanContext, RealmManager realmManager) {
        super(client, previous, type, period, realmManager);
        //todo pass clan context better
        this.clanContext = clanContext;
        this.clanFilterButton = new ClanFilterButton(clanContext, IAbstractClansStatMenu.getClanContexts(client));
        setItem(6, 0, (Item) this.clanFilterButton);
    }
}
