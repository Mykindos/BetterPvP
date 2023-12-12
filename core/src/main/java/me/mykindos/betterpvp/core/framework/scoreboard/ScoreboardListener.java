package me.mykindos.betterpvp.core.framework.scoreboard;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.events.ClientJoinEvent;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

@BPvPListener
public class ScoreboardListener implements Listener {

    private final Core core;

    @Inject
    public ScoreboardListener(Core core){
        this.core = core;
    }

    @EventHandler
    public void onClientJoin(ClientJoinEvent event) {
        event.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    @UpdateEvent(delay = 30000)
    public void updateScoreboards(){
        Bukkit.getOnlinePlayers().forEach(p -> UtilServer.callEvent(new ScoreboardUpdateEvent(p)));
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        UtilServer.runTaskLater(core, () -> UtilServer.callEvent(new ScoreboardUpdateEvent(e.getPlayer())), 10);
    }
}
