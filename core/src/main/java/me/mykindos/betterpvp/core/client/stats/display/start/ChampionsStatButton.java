package me.mykindos.betterpvp.core.client.stats.display.start;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.AbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.ChampionsGameStatMenu;
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

public class ChampionsStatButton extends ControlItem<AbstractStatMenu> {
    @Override
    public ItemProvider getItemProvider(AbstractStatMenu gui) {
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String period = gui.getPeriodKey();

        final GameMapStat winStat = GameMapStat.builder().action(GameMapStat.Action.WIN).build();
        final GameMapStat lossStat = GameMapStat.builder().action(GameMapStat.Action.LOSS).build();
        final GameMapStat matchesPlayedStat = GameMapStat.builder().action(GameMapStat.Action.MATCHES_PLAYED).build();
        final GameMapStat timePlayedStat = GameMapStat.builder().action(GameMapStat.Action.TIME_PLAYED).build();

        final int wins = winStat.getStat(statContainer, period).intValue();
        final int losses = lossStat.getStat(statContainer, period).intValue();
        final int matchesPlayed = matchesPlayedStat.getStat(statContainer, period).intValue();
        final Duration timePlayed = Duration.of(timePlayedStat.getStat(statContainer, period).longValue(), ChronoUnit.MILLIS);

        final List<Component> description = List.of(
                StatFormatterUtility.formatStat("Wins", wins),
                StatFormatterUtility.formatStat("Losses", losses),
                StatFormatterUtility.formatStat("Matches Played", matchesPlayed),
                StatFormatterUtility.formatStat("Time Played", UtilTime.humanReadableFormat(timePlayed))
        );

        return ItemView.builder()
                .material(Material.IRON_SWORD)
                .displayName(Component.text("Champions Stats"))
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
        final AbstractStatMenu gui = getGui();
        new ChampionsGameStatMenu(gui.getClient(), gui, gui.getPeriodKey(), gui.getStatPeriodManager()).show(player);
        //todo
    }
}
