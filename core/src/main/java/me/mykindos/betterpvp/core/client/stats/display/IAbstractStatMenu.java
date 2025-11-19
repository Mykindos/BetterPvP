package me.mykindos.betterpvp.core.client.stats.display;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.client.stats.period.StatPeriodManager;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.logging.menu.button.StringFilterButton;
import me.mykindos.betterpvp.core.menu.Windowed;

import java.util.List;
import java.util.stream.Collectors;

public interface IAbstractStatMenu extends Gui, Windowed {
    Client getClient();
    Windowed getPrevious();
    StatPeriodManager getStatPeriodManager();

    String getPeriodKey();
    void setPeriodKey(String periodKey);

    StringFilterButton<IAbstractStatMenu> getPeriodFilterButton();

    default void updateCurrentPeriod(Windowed previousMenu) {
        if (previousMenu instanceof IAbstractStatMenu abstractStatMenu) {
            abstractStatMenu.setPeriodKey(this.getPeriodKey());
            abstractStatMenu.getPeriodFilterButton().setSelectedFilter(this.getPeriodKey());
        }
        //if previousMenu is instance of IAbstractStatMenu it should always be an AbstractGUI
        if (previousMenu instanceof AbstractGui abstractGui) {
            abstractGui.updateControlItems();
        }
    }

    static List<String> getMapNames(Client client, String gameMode) {
        return client.getStatContainer().getStats().getStatsOfPeriod(StatContainer.GLOBAL_PERIOD_KEY)
                .keySet().stream()
                .filter(stat -> {
                    try {
                        if (!(stat instanceof GameTeamMapStat gameTeamMapStat)) return false;
                        return gameTeamMapStat.getGameName().equals(gameMode);
                    } catch (IllegalArgumentException ignored) {
                        return false;
                    }
                })
                .map(GameTeamMapStat.class::cast)
                .map(GameTeamMapStat::getMapName)
                .collect(Collectors.toSet())
                .stream()
                .sorted()
                .toList();
    }
}
