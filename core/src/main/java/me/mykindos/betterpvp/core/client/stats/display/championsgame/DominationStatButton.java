package me.mykindos.betterpvp.core.client.stats.display.championsgame;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.game.DOMGameStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameMapStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
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
import java.util.List;

public class DominationStatButton extends ControlItem<AbstractStatMenu> {
    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider(AbstractStatMenu gui) {
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String period = gui.getPeriodKey();

        final GameMapStat winStat = GameMapStat.builder().gameName("Domination").action(GameMapStat.Action.WIN).build();
        final GameMapStat lossStat = GameMapStat.builder().gameName("Domination").action(GameMapStat.Action.LOSS).build();
        final GameMapStat matchesPlayedStat = GameMapStat.builder().gameName("Domination").action(GameMapStat.Action.MATCHES_PLAYED).build();
        final GameMapStat timePlayedStat = GameMapStat.builder().gameName("Domination").action(GameMapStat.Action.TIME_PLAYED).build();

        final DOMGameStat killPointsStat = DOMGameStat.builder().action(DOMGameStat.Action.POINTS_KILLS).build();
        final DOMGameStat gemPointsStat = DOMGameStat.builder().action(DOMGameStat.Action.POINTS_GEMS).build();
        final DOMGameStat gemPickupStat = DOMGameStat.builder().action(DOMGameStat.Action.GEMS_PICKED_UP).build();
        final DOMGameStat pointsCapturedStat = DOMGameStat.builder().action(DOMGameStat.Action.CONTROL_POINT_CAPTURED).build();
        final DOMGameStat captureTimeStat = DOMGameStat.builder().action(DOMGameStat.Action.CONTROL_POINT_TIME_CAPTURING).build();
        final DOMGameStat contestTimeStat = DOMGameStat.builder().action(DOMGameStat.Action.CONTROL_POINT_TIME_CONTESTED).build();

        final int wins = winStat.getStat(statContainer, period).intValue();
        final int losses = lossStat.getStat(statContainer, period).intValue();
        final int matchesPlayed = matchesPlayedStat.getStat(statContainer, period).intValue();
        final Duration timePlayed = Duration.of(timePlayedStat.getStat(statContainer, period).longValue(), ChronoUnit.MILLIS);

        final int killPoints = killPointsStat.getStat(statContainer, period).intValue();
        final int gemPoints = gemPointsStat.getStat(statContainer, period).intValue();
        final int gemPickup = gemPickupStat.getStat(statContainer, period).intValue();
        final int pointsCaptured = pointsCapturedStat.getStat(statContainer, period).intValue();
        final Duration captureTime = Duration.of(captureTimeStat.getStat(statContainer, period).longValue(), ChronoUnit.MILLIS);
        final Duration contestTime = Duration.of(contestTimeStat.getStat(statContainer, period).longValue(), ChronoUnit.MILLIS);

        final List<Component> description = List.of(
                StatFormatterUtility.formatStat("Wins", wins),
                StatFormatterUtility.formatStat("Losses", losses),
                StatFormatterUtility.formatStat("Matches Played", matchesPlayed),
                StatFormatterUtility.formatStat("Time Played", UtilTime.humanReadableFormat(timePlayed)),
                Component.empty(),
                StatFormatterUtility.formatStat("Kill Score", killPoints),
                StatFormatterUtility.formatStat("Gem Score", gemPoints),
                StatFormatterUtility.formatStat("Gems Collected", gemPickup),
                StatFormatterUtility.formatStat("Points Captured", pointsCaptured),
                StatFormatterUtility.formatStat("Time Capturing", UtilTime.humanReadableFormat(captureTime)),
                StatFormatterUtility.formatStat("Time Contesting", UtilTime.humanReadableFormat(contestTime))
        );

        return ItemView.builder()
                .material(Material.BEACON)
                .displayName(Component.text("Domination Stats"))
                .lore(description)
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
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
        //todo
    }
}
