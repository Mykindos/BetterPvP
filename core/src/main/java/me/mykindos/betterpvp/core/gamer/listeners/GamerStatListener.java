package me.mykindos.betterpvp.core.gamer.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

@BPvPListener
public class GamerStatListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public GamerStatListener(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onSettingsUpdated(GamerPropertyUpdateEvent event) {
        gamerManager.getGamerRepository().saveProperty(event.getGamer(), event.getProperty(), event.getValue());

        if(event.isUpdateScoreboard()) {
            Player player = Bukkit.getPlayer(UUID.fromString(event.getGamer().getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        }
    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates(){
        gamerManager.getGamerRepository().processStatUpdates(true);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;

        Player player = event.getPlayer();

        gamerManager.getObject(player.getUniqueId()).ifPresent(gamer -> {
            int blocksPlaced = (int) (gamer.getProperty(GamerProperty.BLOCKS_PLACED).orElse(0)) + 1;
            gamer.saveProperty(GamerProperty.BLOCKS_PLACED, blocksPlaced);
        });
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockBroken(BlockBreakEvent event) {
        if(event.isCancelled()) return;
        Player player = event.getPlayer();

        gamerManager.getObject(player.getUniqueId()).ifPresent(gamer -> {
            int blocksBroken = (int) (gamer.getProperty(GamerProperty.BLOCKS_BROKEN).orElse(0)) + 1;
            gamer.saveProperty(GamerProperty.BLOCKS_BROKEN, blocksBroken);
        });

    }


}
