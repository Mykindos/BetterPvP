package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonWrapperStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DungeonClansStatButton extends ControlItem<IAbstractClansStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractClansStatMenu gui) {
        return ItemView.builder()
                .material(Material.MOSSY_COBBLESTONE)
                .lore(getDungeonStats())
                //.action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .displayName(Translations.component("core.menu.stats.clans.button.dungeons.name"))
                .build();
    }

    private List<Component> getDungeonStats() {
        final IAbstractClansStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final StatFilterType type = gui.getType();
        final Period period = gui.getPeriod();
        final String clanName = gui.getClanContext().getClanName();
        final Long clanId = gui.getClanContext().getClanId();

        final ClanWrapperStat.ClanWrapperStatBuilder<?, ?> clanStatBuilder = ClanWrapperStat.builder()
                .clanName(clanName)
                .clanId(clanId);

        //todo manually put normal dungeons
        final ClanWrapperStat dungeonEntersStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.ENTER)
                        .build()
                ).build();
        final ClanWrapperStat dungeonWinsStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.WIN)
                        .build()
                ).build();

        final ClanWrapperStat dungeonLossesStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.LOSS)
                        .build()
                ).build();

        final ClanWrapperStat dungeonTimePlayedStat = clanStatBuilder
                .wrappedStat(DungeonWrapperStat.builder()
                        .wrappedStat(ClientStat.TIME_PLAYED)
                        .build()
                )
                .build();

        final ClanWrapperStat braewoodWinsStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.WIN)
                        .dungeonName("Braewood Caverns")
                        .build()
                ).build();

        final ClanWrapperStat duskmireWinsStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.WIN)
                        .dungeonName("Duskmire Pinnacle")
                        .build()
                ).build();

        final ClanWrapperStat oakmistWinsStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.WIN)
                        .dungeonName("Oakmist Valley")
                        .build()
                ).build();

        final ClanWrapperStat braewoodCitadelWinsStat = clanStatBuilder
                .wrappedStat(DungeonNativeStat.builder()
                        .action(DungeonNativeStat.Action.WIN)
                        .dungeonName("Braewood Citadel")
                        .build()
                ).build();

        final int dungeonWins = dungeonWinsStat.getStat(statContainer, type, period).intValue();
        final int dungeonEnters = dungeonEntersStat.getStat(statContainer, type, period).intValue();
        final int dungeonLosses = dungeonLossesStat.getStat(statContainer, type, period).intValue();
        final String dungeonWinRate = UtilFormat.formatNumber(((double) dungeonWins / (dungeonEnters == 0 ? 1 : dungeonEnters)) * 100, 2) + "%";

        final Duration timePlayed = Duration.of(dungeonTimePlayedStat.getStat(statContainer, type, period), ChronoUnit.MILLIS);

        final int braewoodWins = braewoodWinsStat.getStat(statContainer, type, period).intValue();
        final int duskmireWins = duskmireWinsStat.getStat(statContainer, type, period).intValue();
        final int oakmistWins = oakmistWinsStat.getStat(statContainer, type, period).intValue();
        final int braewoodCitadelWins = braewoodCitadelWinsStat.getStat(statContainer, type, period).intValue();

        return new ArrayList<>(
                List.of(
                        StatFormatterUtility.formatStat("Dungeon Wins", dungeonWins),
                        StatFormatterUtility.formatStat("Dungeon Losses", dungeonLosses),
                        StatFormatterUtility.formatStat("Dungeons Played", dungeonEnters),
                        StatFormatterUtility.formatStat("Dungeon Win Rate", dungeonWinRate),
                        StatFormatterUtility.formatStat("Dungeon Time Played", UtilTime.humanReadableFormat(timePlayed)),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Braewood Caverns Wins", braewoodWins),
                        StatFormatterUtility.formatStat("Duskmire Pinnacle Wins", duskmireWins),
                        StatFormatterUtility.formatStat("Oakmist Valley Wins", oakmistWins),
                        StatFormatterUtility.formatStat("Braewood Citadel Wins", braewoodCitadelWins)

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
        //doesn't show any menu
    }
}
