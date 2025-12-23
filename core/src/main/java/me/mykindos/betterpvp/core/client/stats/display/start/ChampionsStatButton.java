package me.mykindos.betterpvp.core.client.stats.display.start;

import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.display.championsgame.ChampionsGameStatMenu;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.MinecraftStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapWrapperStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@CustomLog
public class ChampionsStatButton extends ControlItem<IAbstractStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {

        return ItemView.builder()
                .material(Material.IRON_SWORD)
                .displayName(Component.text("Champions Stats"))
                .lore(getChampionsStatsDescription("", "", ""))
                .frameLore(true)
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
    }

    protected List<Component> getChampionsStatsDescription(final String gameName, final String teamName, final String mapName) {
        final IAbstractStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final StatFilterType type = gui.getType();
        final Period period = gui.getPeriod();

        final GameTeamMapNativeStat winStat = GameTeamMapNativeStat.builder()
                .gameName(gameName)
                .action(GameTeamMapNativeStat.Action.WIN)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat lossStat = GameTeamMapNativeStat.builder()
                .gameName(gameName)
                .action(GameTeamMapNativeStat.Action.LOSS)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat matchesPlayedStat = GameTeamMapNativeStat.builder()
                .gameName(gameName)
                .action(GameTeamMapNativeStat.Action.MATCHES_PLAYED)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat timePlayedStat = GameTeamMapNativeStat.builder()
                .gameName(gameName)
                .action(GameTeamMapNativeStat.Action.GAME_TIME_PLAYED)
                .teamName(teamName)
                .mapName(mapName)
                .build();
        final GameTeamMapNativeStat lobbyTimePlayedStat = GameTeamMapNativeStat.builder()
                .action(GameTeamMapNativeStat.Action.GAME_TIME_PLAYED)
                .gameName(GameTeamMapStat.LOBBY_GAME_NAME)
                .teamName(teamName)
                .mapName(mapName)
                .build();

        final GameTeamMapWrapperStat killsStat = GameTeamMapWrapperStat.builder()
                .wrappedStat(
                        ClientStat.PLAYER_KILLS
                )
                .gameName(gameName)
                .teamName(teamName)
                .mapName(mapName)
                .build();

        final GameTeamMapWrapperStat deathsStat = GameTeamMapWrapperStat.builder()
                .wrappedStat(
                        MinecraftStat.builder()
                                .statistic(Statistic.DEATHS)
                                .build()
                )
                .gameName(gameName)
                .teamName(teamName)
                .mapName(mapName)
                .build();


        final int wins = winStat.getStat(statContainer, type, period).intValue();
        final int losses = lossStat.getStat(statContainer, type, period).intValue();
        final int matchesPlayed = matchesPlayedStat.getStat(statContainer, type, period).intValue();

        final int kills = killsStat.getStat(statContainer, type, period).intValue();
        final int deaths = deathsStat.getStat(statContainer, type, period).intValue();
        final float killDeathRatio = (float) kills / (deaths == 0 ? 1 : deaths);


        Duration timePlayed = Duration.of(timePlayedStat.getStat(statContainer, type, period), ChronoUnit.MILLIS);
        if (gameName.isEmpty()) {
            //subtract lobby time from overall time
            timePlayed = timePlayed.minus(lobbyTimePlayedStat.getStat(statContainer, type, period), ChronoUnit.MILLIS);
        }

        return new ArrayList<>(
                List.of(
                        StatFormatterUtility.formatStat("Wins", wins),
                        StatFormatterUtility.formatStat("Losses", losses),
                        StatFormatterUtility.formatStat("Matches Played", matchesPlayed),
                        StatFormatterUtility.formatStat("Kills", kills),
                        StatFormatterUtility.formatStat("Deaths", deaths),
                        StatFormatterUtility.formatStat("K/D", killDeathRatio),
                        StatFormatterUtility.formatStat("Time Played", UtilTime.humanReadableFormat(timePlayed))
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
        final IAbstractStatMenu gui = getGui();
        new ChampionsGameStatMenu(gui.getClient(), gui, gui.getType(), gui.getPeriod(), gui.getRealmManager()).show(player);
    }
}
