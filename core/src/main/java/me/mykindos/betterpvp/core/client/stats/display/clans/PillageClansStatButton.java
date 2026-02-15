package me.mykindos.betterpvp.core.client.stats.display.clans;

import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.IAbstractClansStatMenu;
import me.mykindos.betterpvp.core.client.stats.display.StatFormatterUtility;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.inventory.item.ItemProvider;
import me.mykindos.betterpvp.core.inventory.item.impl.controlitem.ControlItem;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PillageClansStatButton extends ControlItem<IAbstractClansStatMenu> {
    @Override
    public ItemProvider getItemProvider(IAbstractClansStatMenu gui) {
        return ItemView.builder()
                .material(Material.END_CRYSTAL)
                .lore(getPillageStats())
                //.action(ClickActions.ALL, Component.text("Show Detailed Stats"))
                .displayName(Component.text("Pillage Stats"))
                .build();
    }

    private List<Component> getPillageStats() {
        final IAbstractClansStatMenu gui = getGui();
        final StatContainer statContainer = gui.getClient().getStatContainer();
        final StatFilterType type = gui.getType();
        final Period period = gui.getPeriod();
        final String clanName = gui.getClanContext().getClanName();
        final Long clanId = gui.getClanContext().getClanId();

        final ClanWrapperStat.ClanWrapperStatBuilder<?, ?> clanStatBuilder = ClanWrapperStat.builder()
                .clanName(clanName)
                .clanId(clanId);

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

        final ClanWrapperStat coreDamageStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_CORE_DAMAGE)
                .build();

        final ClanWrapperStat cannonShotsStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_CANNON_SHOT)
                .build();

        final ClanWrapperStat cannonBlockDamageStat = clanStatBuilder
                .wrappedStat(ClientStat.CLANS_CANNON_BLOCK_DAMAGE)
                .build();

        final int pillagesAttacked = pillageAttackStat.getStat(statContainer, type, period).intValue();
        final int coresDestroy = coreDestroyStat.getStat(statContainer, type, period).intValue();
        final String winRate = UtilFormat.formatNumber(((double) pillagesAttacked / (coresDestroy == 0 ? 1 : coresDestroy) ) * 100, 2) + "%";
        final double coreDamage = coreDamageStat.getDoubleStat(statContainer, type, period);
        final int cannonShots = cannonShotsStat.getStat(statContainer, type, period).intValue();
        final int cannonBlockDamage = cannonBlockDamageStat.getStat(statContainer, type, period).intValue();

        final int pillagesDefended = pillageDefendStat.getStat(statContainer, type, period).intValue();
        final int coresDestroyed = coreDestroyedStat.getStat(statContainer, type, period).intValue();
        final String lossRate = UtilFormat.formatNumber(((double) pillagesDefended / (coresDestroyed == 0 ? 1 : coresDestroyed)) * 100, 2) + "%";

        return new ArrayList<>(
                List.of(
                        StatFormatterUtility.formatStat("Offensive Pillages", pillagesAttacked),
                        StatFormatterUtility.formatStat("Enemy Core Destroyed", coresDestroy),
                        StatFormatterUtility.formatStat("Pillage Win Rate", winRate),
                        StatFormatterUtility.formatStat("Enemy Core Damage", coreDamage),
                        StatFormatterUtility.formatStat("Cannon Shots", cannonShots),
                        StatFormatterUtility.formatStat("Blocks Destroyed by Cannon", cannonBlockDamage),
                        Component.empty(),
                        StatFormatterUtility.formatStat("Defensive Pillages", pillagesDefended),
                        StatFormatterUtility.formatStat("Own Core Destroyed", coresDestroyed),
                        StatFormatterUtility.formatStat("Pillage Loss Rate", lossRate)
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
        //doesn't show a menu
    }
}
