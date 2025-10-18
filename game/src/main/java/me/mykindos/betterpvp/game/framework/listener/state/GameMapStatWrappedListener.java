package me.mykindos.betterpvp.game.framework.listener.state;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.stats.events.WrapStatEvent;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapWrapperStat;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.game.framework.model.stats.StatManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@BPvPListener
@Singleton
@CustomLog
public class GameMapStatWrappedListener implements Listener {
    private final StatManager statManager;
    @Inject
    public GameMapStatWrappedListener(StatManager statManager) {
        this.statManager = statManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onStatUpdate(WrapStatEvent event) {
        log.info("Wrap stat start {}", event.getStat()).submit();
        if (event.getStat() instanceof GameTeamMapStat) return;
        //already wrap time played and split it into spectate time
        //todo check this
        if (ClientStat.TIME_PLAYED.equals(event.getStat())) return;
        final GameTeamMapWrapperStat.GameTeamMapWrapperStatBuilder<?, ?> builder = GameTeamMapWrapperStat.builder()
                .wrappedStat(event.getStat());
        final GameTeamMapWrapperStat wrappedStat = (GameTeamMapWrapperStat) statManager.addGameMapStatElements(event.getId(), builder).build();
        event.setStat(wrappedStat);
        log.info("Wrap stat end {}", event.getStat()).submit();
    }
}
