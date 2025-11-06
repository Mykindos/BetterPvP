package me.mykindos.betterpvp.core.client.stats.display.start;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.display.clans.ClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.MinecraftStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.events.BossStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.model.item.ClickActions;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClansStatButton<T extends IAbstractStatMenu> extends ControlItem<T> {
    /**
     * Gets the {@link ItemProvider}.
     * This method gets called every time a {@link Window} is notified ({@link #notifyWindows()}).
     *
     * @return The {@link ItemProvider}
     */
    @Override
    public ItemProvider getItemProvider(IAbstractStatMenu gui) {
        String clanName = "";
        UUID clanId = null;
        if (gui instanceof IAbstractClansStatMenu clanGui) {
            clanName = clanGui.getClanContext().getClanName();
            clanId = clanGui.getClanContext().getClanId();
        }
        return ItemView.builder()
                .material(Material.TNT)
                .lore(getClanStats(clanName, clanId))
                .action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .displayName(Component.text("Clans Stats"))
                .build();
    }

    protected List<Component> getClanStats(String clanName, @Nullable UUID clanId) {
        final IAbstractStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String periodKey = gui.getPeriodKey();

        final ClanWrapperStat.ClanWrapperStatBuilder<?, ?> clanStatBuilder = ClanWrapperStat.builder()
                        .clanName(clanName)
                        .clanId(clanId);

        final ClanWrapperStat killsStat = clanStatBuilder
                .wrappedStat(ClientStat.PLAYER_KILLS)
                .build();

        final ClanWrapperStat deathsStat = clanStatBuilder
                .wrappedStat(MinecraftStat.builder()
                        .statistic(Statistic.DEATHS)
                        .entityType(EntityType.PLAYER)
                        .build()
                ).build();

        final ClanWrapperStat timePlayedStat = clanStatBuilder
                .wrappedStat(ClientStat.TIME_PLAYED)
                .build();

        final ClanWrapperStat dominanceGainedStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_DOMINANCE_GAINED)
                .build();
        final ClanWrapperStat dominanceLostStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_DOMINANCE_LOST)
                .build();
        final ClanWrapperStat pillageAttackStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_ATTACK_PILLAGE)
                .build();
        final ClanWrapperStat pillageDefendStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_DEFEND_PILLAGE)
                .build();
        final ClanWrapperStat coreDestroyStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_DESTROY_CORE)
                .build();
        final ClanWrapperStat coreDestroyedStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_CORE_DESTROYED)
                .build();

        final ClanWrapperStat eventBossesKilledStat = clanStatBuilder
                .wrappedStat(BossStat.builder()
                        .action(BossStat.Action.KILL)
                        .build()
                ).build();
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

        final int kills = killsStat.getStat(statContainer, periodKey).intValue();
        final int deaths = deathsStat.getStat(statContainer, periodKey).intValue();
        final float killDeathRatio = (float) kills / (deaths == 0 ? 1 : deaths);

        final Duration timePlayed = Duration.of(timePlayedStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);

        final double dominanceGained = dominanceGainedStat.getStat(statContainer, periodKey);
        final double dominanceLost = dominanceLostStat.getStat(statContainer, periodKey);
        final double dominanceDelta = dominanceGained - dominanceLost;

        final int pillagesAttacked = pillageAttackStat.getStat(statContainer, periodKey).intValue();
        final int coresDestroy = coreDestroyStat.getStat(statContainer, periodKey).intValue();
        final String winRate = UtilFormat.formatNumber(((double) pillagesAttacked / (coresDestroy == 0 ? 1 : coresDestroy) ) * 100, 2) + "%";

        final int pillagesDefended = pillageDefendStat.getStat(statContainer, periodKey).intValue();
        final int coresDestroyed = coreDestroyedStat.getStat(statContainer, periodKey).intValue();
        final String lossRate = UtilFormat.formatNumber(((double) pillagesDefended / (coresDestroyed == 0 ? 1 : coresDestroyed)) * 100, 2) + "%";

        final int eventBossesKilled = eventBossesKilledStat.getStat(statContainer, periodKey).intValue();
        final int dungeonWins = dungeonWinsStat.getStat(statContainer, periodKey).intValue();
        final int dungeonEnters = dungeonEntersStat.getStat(statContainer, periodKey).intValue();
        final String dungeonWinRate = UtilFormat.formatNumber(((double) dungeonWins / (dungeonEnters == 0 ? 1 : dungeonEnters)) * 100, 2) + "%";


        return new ArrayList<>(
                List.of(
                        StatFormatterUtility.formatStat("Kills", kills),
                        StatFormatterUtility.formatStat("Deaths", deaths),
                        StatFormatterUtility.formatStat("K/D", killDeathRatio),
                        StatFormatterUtility.formatStat("Time Played", UtilTime.humanReadableFormat(timePlayed)),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Dominance Gained", dominanceGained),
                        StatFormatterUtility.formatStat("Dominance Lost", dominanceLost),
                        StatFormatterUtility.formatStat("Dominance Change", dominanceDelta),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Pillage Win Rate", winRate),
                        StatFormatterUtility.formatStat("Pillage Loss Rate", lossRate),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Event Boss Kills", eventBossesKilled),
                        StatFormatterUtility.formatStat("Dungeon Wins", dungeonWins),
                        StatFormatterUtility.formatStat("Dungeon Win Rate", dungeonWinRate)
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
        IAbstractStatMenu gui = getGui();
        new ClansStatMenu(gui.getClient(), gui, gui.getPeriodKey(), gui.getStatPeriodManager()).show(player);
    }
}
