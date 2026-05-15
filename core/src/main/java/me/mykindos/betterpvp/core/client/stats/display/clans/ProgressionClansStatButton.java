package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProgressionClansStatButton extends ControlItem<IAbstractClansStatMenu> {

    @Override
    public ItemProvider getItemProvider(IAbstractClansStatMenu gui) {
        return ItemView.builder()
                .material(Material.FISHING_ROD)
                .lore(getProgressionStats())
                .displayName(Component.text("Profession Stats"))
                .build();
    }

    private List<Component> getProgressionStats() {
        final IAbstractClansStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final StatFilterType type = gui.getType();
        final Period period = gui.getPeriod();
        final String clanName = gui.getClanContext().getClanName();
        final Long clanId = gui.getClanContext().getClanId();

        final ClanWrapperStat.ClanWrapperStatBuilder<?, ?> clanStatBuilder = ClanWrapperStat.builder()
                .clanName(clanName)
                .clanId(clanId);

        final ClanWrapperStat fishCaughtStat = clanStatBuilder
                .wrappedStat(ClientStat.FISH_CAUGHT)
                .build();

        final ClanWrapperStat logsChoppedStat = clanStatBuilder
                .wrappedStat(ClientStat.LOG_CHOPPED)
                .build();

        final ClanWrapperStat fishingXpStat = clanStatBuilder
                .wrappedStat(ClientStat.FISHING_XP)
                .build();

        final ClanWrapperStat woodcuttingXpStat = clanStatBuilder
                .wrappedStat(ClientStat.WOODCUTTING_XP)
                .build();

        final ClanWrapperStat oresMinedStat = clanStatBuilder
                .wrappedStat(ClientStat.ORE_MINED)
                .build();

        final ClanWrapperStat miningXpStat = clanStatBuilder
                .wrappedStat(ClientStat.MINING_XP)
                .build();

        final int fishCaught = fishCaughtStat.getStat(statContainer, type, period).intValue();
        final int logsChopped = logsChoppedStat.getStat(statContainer, type, period).intValue();
        final int oresMined = oresMinedStat.getStat(statContainer, type, period).intValue();
        final double fishingXp = fishingXpStat.getDoubleStat(statContainer, type, period);
        final double woodcuttingXp = woodcuttingXpStat.getDoubleStat(statContainer, type, period);
        final double miningXp = miningXpStat.getDoubleStat(statContainer, type, period);

        return new ArrayList<>(
                List.of(
                        StatFormatterUtility.formatStat("Fish Caught", fishCaught),
                        StatFormatterUtility.formatStat("Fishing XP", UtilFormat.formatNumber(fishingXp, 1)),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Logs Chopped", logsChopped),
                        StatFormatterUtility.formatStat("Woodcutting XP", UtilFormat.formatNumber(woodcuttingXp, 1)),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Ores Mined", oresMined),
                        StatFormatterUtility.formatStat("Mining XP", UtilFormat.formatNumber(miningXp, 1))
                )
        );
    }

    @Override
    public void handleClick(@NotNull ClickType clickType, @NotNull Player player, @NotNull InventoryClickEvent event) {
        // doesn't show a menu
    }
}


