package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventClansStatButton extends ControlItem<IAbstractClansStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractClansStatMenu gui) {
        return ItemView.builder()
                .material(Material.TNT)
                .lore(getEventStats())
                //.action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .displayName(Component.text("Clans Stats"))
                .build();
    }

    private List<Component> getEventStats() {
        final IAbstractClansStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String periodKey = gui.getPeriodKey();
        final String clanName = gui.getClanContext().getClanName();
        final UUID clanId = gui.getClanContext().getClanId();

        final ClanWrapperStat.ClanWrapperStatBuilder<?, ?> clanStatBuilder = ClanWrapperStat.builder()
                .clanName(clanName)
                .clanId(clanId);

        //todo manually put normal event bosses i.e. dreadbeard
        final ClanWrapperStat eventBossesKilledStat = clanStatBuilder
                .wrappedStat(BossStat.builder()
                        .action(BossStat.Action.KILL)
                        .build()
                ).build();

        final ClanWrapperStat undeadChestsOpenedStat = clanStatBuilder
                .wrappedStat(ClientStat.EVENT_UNDEAD_CITY_OPEN_CHEST)
                .build();

        final int eventBossesKilled = eventBossesKilledStat.getStat(statContainer, periodKey).intValue();
        final int undeadChestsOpened = undeadChestsOpenedStat.getStat(statContainer, periodKey).intValue();

        return new ArrayList<>(
                List.of(
                        StatFormatterUtility.formatStat("Total Bosses Killed", eventBossesKilled),
                        StatFormatterUtility.formatStat("Undead City Chests Opened", undeadChestsOpened)
                )
        );

    }

    /**
     * A method called if the {@link ItemStack} associated to this {@link Item}
     * has been clicked by a player.
     *
     * @param clickType The {@link ClickType} the {@link Player} performed.
     * @param player    The {@link Player} who clicked on the {@link ItemStack}.
     * @param event     The {@link InventoryClickEvent} associated with this click.
     */
    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {

    }
}
