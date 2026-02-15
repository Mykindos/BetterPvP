package me.mykindos.betterpvp.core.client.stats.display;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.filter.ClanContext;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClansStat;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.menu.button.filter.IContextFilterButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface IAbstractClansStatMenu extends IAbstractStatMenu {
    IContextFilterButton<ClanContext> getClanFilterButton();
    ClanContext getClanContext();
    void setClanContext(ClanContext newContext);

    default void updateCurrentClanContext(Windowed previousMenu) {
        if (previousMenu instanceof IAbstractClansStatMenu clansAbstractStatMenu) {
            clansAbstractStatMenu.setClanContext(this.getClanContext());
            clansAbstractStatMenu.getClanFilterButton().setSelectedFilter(this.getClanContext());
        }
        //if previousMenu is instance of IAbstractStatMenu it should always be an AbstractGUI
        if (previousMenu instanceof AbstractGui abstractGui) {
            abstractGui.updateControlItems();
        }
    }

    static List<ClanContext> getClanContexts(Client client) {
        List<ClanContext> contexts = new ArrayList<>(List.of(
                ClanContext.ALL
        ));
        contexts.addAll(
                client.getStatContainer().getStats().getStatsOfPeriod(StatFilterType.ALL, null)
                        .keySet().stream()
                        .filter(ClansStat.class::isInstance)
                        .map(ClansStat.class::cast)
                        .map(ClanContext::from)
                        .collect(Collectors.toSet())
        );
        contexts.sort(null);
        return contexts;
    }
}
