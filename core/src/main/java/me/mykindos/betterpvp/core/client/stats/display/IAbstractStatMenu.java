package me.mykindos.betterpvp.core.client.stats.display;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.display.filter.RealmContext;
import me.mykindos.betterpvp.core.client.stats.display.filter.RealmFilterButton;
import me.mykindos.betterpvp.core.client.stats.display.filter.SeasonContext;
import me.mykindos.betterpvp.core.client.stats.display.filter.SeasonFilterButton;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.inventory.gui.AbstractGui;
import me.mykindos.betterpvp.core.inventory.gui.Gui;
import me.mykindos.betterpvp.core.menu.Windowed;
import me.mykindos.betterpvp.core.server.Period;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public interface IAbstractStatMenu extends Gui, Windowed {
    Client getClient();
    Windowed getPrevious();
    RealmManager getRealmManager();

    StatFilterType getType();
    void setType(StatFilterType type);
    @Nullable
    Period getPeriod();
    void setPeriod(@Nullable Period period);

    SeasonFilterButton getSeasonFilterButton();
    RealmFilterButton getRealmFilterButton();

    default void updateCurrentPeriod(Windowed previousMenu) {
        if (previousMenu instanceof IAbstractStatMenu abstractStatMenu) {
            abstractStatMenu.setType(this.getType());
            abstractStatMenu.setPeriod(this.getPeriod());
        }
        //if previousMenu is instance of IAbstractStatMenu it should always be an AbstractGUI
        if (previousMenu instanceof AbstractGui abstractGui) {
            abstractGui.updateControlItems();
        }
    }

    static List<RealmContext> getRealmContexts(Season season, RealmManager realmManager) {
        if (season == null) return new ArrayList<>(List.of(RealmContext.ALL));
        List<RealmContext> newRealmContexts = new ArrayList<>(realmManager.getRealmsBySeason(season)
                .stream()
                .map(RealmContext::new)
                .toList());
        newRealmContexts.add(RealmContext.ALL);
        return newRealmContexts;
    }

    static List<SeasonContext> getSeasonContexts(RealmManager realmManager) {
        List<SeasonContext> newRealmContexts = new ArrayList<>(realmManager.getSeasonMap().values()
                .stream()
                .map(SeasonContext::new)
                .toList());
        newRealmContexts.add(SeasonContext.ALL);
        return newRealmContexts;
    }

    @NotNull
    static SeasonContext getSeasonContext(@NotNull StatFilterType type, @Nullable Period period) {
        if (type == StatFilterType.ALL) {
            return SeasonContext.ALL;
        }
        if (period instanceof Season season) {
            return new SeasonContext(season);
        }
        if (period instanceof Realm realm) {
            return new SeasonContext(realm.getSeason());
        }
        throw new IllegalStateException();
    }

    @NotNull
    static RealmContext getRealmContext(@NotNull StatFilterType type, @Nullable Period period) {
        if (type == StatFilterType.ALL || type == StatFilterType.SEASON) {
            return RealmContext.ALL;
        }
        if (period instanceof Realm realm) {
            return new RealmContext(realm);
        }
        throw new IllegalStateException();
    }


    static List<String> getMapNames(Client client, String gameMode) {
        return client.getStatContainer().getStats().getStatsOfPeriod(StatFilterType.ALL, null)
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
