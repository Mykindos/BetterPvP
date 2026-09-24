package me.mykindos.betterpvp.clans.world.camp.hall;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.structure.CampUpgrades;
import me.mykindos.betterpvp.clans.world.camp.upgrade.FeastTable;
import me.mykindos.betterpvp.clans.world.camp.upgrade.GreatBell;
import me.mykindos.betterpvp.clans.world.camp.upgrade.ProsperityBreakdown;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** Gives the Feast table, Prosperity breakdown and Great bell their pages, when the plugin starts. */
@BPvPListener
@Singleton
public class MoraleUpgradePages implements Listener {

    @Inject
    public MoraleUpgradePages(@NotNull HallMenus menus, @NotNull CampUpgrades upgrades, @NotNull FeastTable feast,
                              @NotNull ProsperityBreakdown breakdown, @NotNull GreatBell bell) {
        upgrades.page(FeastTable.ID, (player, camp, structure, previous) ->
                new FeastTableMenu(menus, feast, player, camp, previous).show(player));
        upgrades.page(ProsperityBreakdown.ID, (player, camp, structure, previous) ->
                new ProsperityBreakdownMenu(breakdown.factors(camp), previous).show(player));
        upgrades.page(GreatBell.ID, (player, camp, structure, previous) ->
                new GreatBellMenu(menus, bell, camp, structure, previous).show(player));
    }
}
