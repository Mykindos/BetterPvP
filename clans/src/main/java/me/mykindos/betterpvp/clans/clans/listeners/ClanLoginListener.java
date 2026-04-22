package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.client.stats.RealmManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
public class ClanLoginListener implements Listener {
    private final RealmManager realmManager;

    @Inject
    public ClanLoginListener(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    @EventHandler()
    public void onLogin(ClientJoinEvent event) {
        //final ClanWrapperStat timePlayedStat = ClanWrapperStat.builder()
        //        .wrappedStat(ClientStat.TIME_PLAYED)
        //        .build();
        //final Season season = Core.getCurrentRealm().getSeason();
        //long timePlayed = timePlayedStat.getStat(event.getClient().getStatContainer(), StatFilterType.SEASON, season);
        //if (timePlayed <= 1000 * 60 * 60) {
        //
//
        //}
    }
}
