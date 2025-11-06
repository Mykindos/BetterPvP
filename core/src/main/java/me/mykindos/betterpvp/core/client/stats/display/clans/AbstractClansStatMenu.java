package me.mykindos.betterpvp.core.client.stats.display.clans;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.filter.ClanContext;
import me.mykindos.betterpvp.core.client.stats.display.filter.ClanFilterButton;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.item.Item;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.filter.IContextFilterButton;
import org.bukkit.Material;
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
    protected AbstractClansStatMenu(@NotNull Client client, @Nullable Windowed previous, String periodKey, StatPeriodManager statPeriodManager) {
        super(client, previous, periodKey, statPeriodManager);
        //todo pass clan context better
        this.clanContext = ClanContext.ALL;
        this.clanFilterButton = new ClanFilterButton("Clan", ClanContext.ALL, IAbstractClansStatMenu.getClanContexts(client), 9, Material.IRON_DOOR, 0);
        setItem(7, 0, (Item) this.clanFilterButton);
    }
}
