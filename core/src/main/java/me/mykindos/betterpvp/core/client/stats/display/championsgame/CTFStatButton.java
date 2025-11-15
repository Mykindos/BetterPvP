package me.mykindos.betterpvp.core.client.stats.display.championsgame;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.ctf.CTFStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.start.ChampionsStatButton;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CTFStatButton extends ChampionsStatButton {
    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {
        return ItemView.builder()
                .material(Material.WHITE_BANNER)
                .lore(getCTFStatsDescription("", ""))
                .displayName(Component.text("Capture The Flag Stats"))
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
    }

    /**
     *
     * @param teamName {@code ""} if empty
     * @param mapName {@code ""} if empty
     * @return
     */
    protected List<Component> getCTFStatsDescription(final String teamName, final String mapName) {
        final IAbstractStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String periodKey = gui.getPeriodKey();

        final String gameName = "Capture The Flag";

        final GameTeamMapNativeStat flagCapturesStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.FLAG_PICKUP)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat flagPickupsStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.FLAG_PICKUP)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat flagKillsStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.FLAG_CARRIER_KILLS)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat suddenKillsStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.SUDDEN_DEATH_KILLS)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat suddenDeathsStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.SUDDEN_DEATH_DEATHS)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat suddenCapturesStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.SUDDEN_DEATH_FLAG_CAPTURES)
                .teamName(teamName)
                .mapName(mapName)
                .build();

        final int flagCaptures = flagCapturesStat.getStat(statContainer, periodKey).intValue();
        final int flagPickups = flagPickupsStat.getStat(statContainer, periodKey).intValue();
        final int flagKills = flagKillsStat.getStat(statContainer, periodKey).intValue();

        final int suddenKills = suddenKillsStat.getStat(statContainer, periodKey).intValue();
        final int suddenDeaths = suddenDeathsStat.getStat(statContainer, periodKey).intValue();
        final int suddenCaptures = suddenCapturesStat.getStat(statContainer, periodKey).intValue();

        final List<Component> dominationStats = new ArrayList<>(
                getChampionsStatsDescription(gameName, teamName, mapName)

        );
        dominationStats.addAll(List.of(
                        Component.empty(),
                        StatFormatterUtility.formatStat("Flag Captures", flagCaptures),
                        StatFormatterUtility.formatStat("Flag Pickups", flagPickups),
                        StatFormatterUtility.formatStat("Flag Carrier Kills", flagKills),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Sudden Death Kills", suddenKills),
                        StatFormatterUtility.formatStat("Sudden Death Deaths", suddenDeaths),
                        StatFormatterUtility.formatStat("Sudden Death Captures", suddenCaptures)
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
        new CTFStatMenu(gui.getClient(), gui, gui.getPeriodKey(), gui.getStatPeriodManager()).show(player);
    }
}
