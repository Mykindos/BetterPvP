package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.core.MinecraftStat;
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
import java.util.UUID;

public class PersonalClansStatButton extends ControlItem<IAbstractClansStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractClansStatMenu gui) {
        return ItemView.builder()
                .material(Material.IRON_SWORD)
                .lore(getPersonalStats())
                //.action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .displayName(Component.text("Personal Stats"))
                .build();
    }

    private List<Component> getPersonalStats() {
        final IAbstractClansStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final String periodKey = gui.getPeriodKey();
        final String clanName = gui.getClanContext().getClanName();
        final UUID clanId = gui.getClanContext().getClanId();

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

        final ClanWrapperStat clanExperienceStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_CLANS_EXPERIENCE)
                .build();

        final ClanWrapperStat clanEnergyDropStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_ENERGY_DROPPED)
                .build();

        final ClanWrapperStat clanEnergyCollectedStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_ENERGY_COLLECTED)
                .build();

        final int kills = killsStat.getStat(statContainer, periodKey).intValue();
        final int deaths = deathsStat.getStat(statContainer, periodKey).intValue();
        final float killDeathRatio = (float) kills / (deaths == 0 ? 1 : deaths);

        final Duration timePlayed = Duration.of(timePlayedStat.getStat(statContainer, periodKey).longValue(), ChronoUnit.MILLIS);

        final double dominanceGained = dominanceGainedStat.getStat(statContainer, periodKey);
        final double dominanceLost = dominanceLostStat.getStat(statContainer, periodKey);
        final double dominanceDelta = dominanceGained - dominanceLost;

        final double clanExperience = clanExperienceStat.getStat(statContainer, periodKey);

        final double energyDrop = clanEnergyDropStat.getStat(statContainer, periodKey);
        final double energyCollected = clanEnergyCollectedStat.getStat(statContainer, periodKey);

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
                        StatFormatterUtility.formatStat("Clan Experience Earned", clanExperience),
                        StatFormatterUtility.formatStat("Energy Dropped", energyDrop),
                        StatFormatterUtility.formatStat("Energy Collected", energyCollected)
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

    }
}
