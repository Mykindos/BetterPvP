package me.mykindos.betterpvp.core.client.stats.display.start;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.MinecraftStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class GenericStatButton extends ControlItem<IAbstractStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {

        return ItemView.builder()
                .material(Material.ANVIL)
                .displayName(Component.text("Generic Stats"))
                .lore(getGenericStatsDescription())
                .frameLore(true)
                //.action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .build();
    }

    protected List<Component> getGenericStatsDescription() {
        final IAbstractStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String periodKey = gui.getPeriodKey();

        final GenericStat timePlayedStat = new GenericStat(ClientStat.TIME_PLAYED);
        final GenericStat spectateTimePlayedStat = new GenericStat(
                        GameTeamMapNativeStat.builder()
                                .action(GameTeamMapNativeStat.Action.SPECTATE_TIME)
                                .build()
                );

        final GenericStat lobbyTimePlayedStat = new GenericStat(
                    GameTeamMapNativeStat.builder()
                    .action(GameTeamMapNativeStat.Action.GAME_TIME_PLAYED)
                    .gameName(GameTeamMapStat.LOBBY_GAME_NAME)
                    .build()
        );

        final GenericStat killsStat = new GenericStat(
                MinecraftStat.builder()
                .statistic(Statistic.KILL_ENTITY)
                .entityType(EntityType.PLAYER)
                .build()
        );

        final GenericStat deathsStat = new GenericStat(MinecraftStat.builder()
                .statistic(Statistic.DEATHS)
                .build()
        );

        final int kills = killsStat.getStat(statContainer, periodKey).intValue();
        final int deaths = deathsStat.getStat(statContainer, periodKey).intValue();
        final float killDeathRatio = (float) kills / (deaths == 0 ? 1 : deaths);


        Duration timePlayed = Duration.of(timePlayedStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);
        //subtract game spectate time from overall time
        timePlayed = timePlayed.minus(spectateTimePlayedStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);
        //subtract lobby time from overall time
        timePlayed = timePlayed.minus(lobbyTimePlayedStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);


        return new ArrayList<>(
                List.of(
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
        //todo figure out how to show non-featured stats
    }
}
