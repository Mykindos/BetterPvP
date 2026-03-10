package me.mykindos.betterpvp.core.client.stats.display.championsgame;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.domination.DominationStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.start.ChampionsStatButton;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.server.Period;
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
@CustomLog
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
        final StatFilterType type = gui.getType();
        final Period period = gui.getPeriod();

        final String gameName = "Domination";

        final GameTeamMapNativeStat.GameTeamMapNativeStatBuilder<?, ?> builder = GameTeamMapNativeStat.builder()
                .gameName(gameName)
                .teamName(teamName)
                .mapName(mapName);

        final GameTeamMapNativeStat killPointsStat = builder
                .action(GameTeamMapNativeStat.Action.POINTS_KILLS)
                .build();
        final GameTeamMapNativeStat gemPointsStat = builder
                .action(GameTeamMapNativeStat.Action.POINTS_GEMS)
                .build();
        final GameTeamMapNativeStat gemPickupStat = builder
                .action(GameTeamMapNativeStat.Action.GEMS_PICKED_UP)
                .build();
        final GameTeamMapNativeStat pointsCapturedStat = builder
                .action(GameTeamMapNativeStat.Action.CONTROL_POINT_CAPTURED)
                .build();
        final GameTeamMapNativeStat captureTimeStat = builder
                .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CAPTURING)
                .build();
        final GameTeamMapNativeStat contestTimeStat = builder
                .action(GameTeamMapNativeStat.Action.CONTROL_POINT_TIME_CONTESTED)
                .build();

        final int killPoints = killPointsStat.getStat(statContainer, type, period).intValue();
        final int gemPoints = gemPointsStat.getStat(statContainer, type, period).intValue();
        final int gemPickup = gemPickupStat.getStat(statContainer, type, period).intValue();
        final int pointsCaptured = pointsCapturedStat.getStat(statContainer, type, period).intValue();
        final Duration captureTime = Duration.of(captureTimeStat.getStat(statContainer, type, period), ChronoUnit.MILLIS);
        final Duration contestTime = Duration.of(contestTimeStat.getStat(statContainer, type, period), ChronoUnit.MILLIS);

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
        new DominationStatMenu(gui.getClient(), gui, gui.getType(), gui.getPeriod(), gui.getRealmManager()).show(player);
    }
}
