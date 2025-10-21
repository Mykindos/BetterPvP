package me.mykindos.betterpvp.core.client.stats.display.championsgame;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.DominationStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.start.ChampionsStatButton;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
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

public class DominationStatButton extends ChampionsStatButton {
    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {

        return ItemView.builder()
                .material(Material.BEACON)
                .displayName(Component.text("Domination Stats"))
                .lore(getDominationStatsDescription("", ""))
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
    }

    /**
     *
     * @param teamName {@code ""} if empty
     * @param mapName {@code ""} if empty
     * @return
     */
    protected List<Component> getDominationStatsDescription(final String teamName, final String mapName) {
        final IAbstractStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String periodKey = gui.getPeriodKey();

        final String gameName = "Domination";

        final GameTeamMapNativeStat killPointsStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.POINTS_KILLS)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat gemPointsStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.POINTS_GEMS)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat gemPickupStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.GEMS_PICKED_UP)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat pointsCapturedStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.CONTROL_POINT_CAPTURED)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat captureTimeStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CAPTURING)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat contestTimeStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CONTESTED)
                .teamName(teamName)
                .mapName(mapName)
                .build();

        final int killPoints = killPointsStat.getStat(statContainer, periodKey).intValue();
        final int gemPoints = gemPointsStat.getStat(statContainer, periodKey).intValue();
        final int gemPickup = gemPickupStat.getStat(statContainer, periodKey).intValue();
        final int pointsCaptured = pointsCapturedStat.getStat(statContainer, periodKey).intValue();
        final Duration captureTime = Duration.of(captureTimeStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);
        final Duration contestTime = Duration.of(contestTimeStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);

        final List<Component> dominationStats = new ArrayList<>(
                getChampionsStatsDescription(gameName, teamName, mapName)

        );
        dominationStats.addAll(List.of(
                Component.empty(),
                StatFormatterUtility.formatStat("Kill Score", killPoints),
                StatFormatterUtility.formatStat("Gem Score", gemPoints),
                StatFormatterUtility.formatStat("Gems Collected", gemPickup),
                StatFormatterUtility.formatStat("Points Captured", pointsCaptured),
                StatFormatterUtility.formatStat("Time Capturing", UtilTime.humanReadableFormat(captureTime)),
                StatFormatterUtility.formatStat("Time Contesting", UtilTime.humanReadableFormat(contestTime))
            )
        );

        return dominationStats;
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
        final IAbstractStatMenu gui = getGui();
        new DominationStatMenu(gui.getClient(), gui, gui.getPeriodKey(), gui.getStatPeriodManager()).show(player);
    }
}
