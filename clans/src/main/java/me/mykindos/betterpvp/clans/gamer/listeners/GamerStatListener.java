package me.mykindos.betterpvp.clans.gamer.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.map.nms.UtilMapMaterial;
import me.mykindos.betterpvp.clans.gamer.Gamer;
import me.mykindos.betterpvp.clans.gamer.GamerManager;
import me.mykindos.betterpvp.clans.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.settings.events.SettingsUpdatedEvent;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

@BPvPListener
public class GamerStatListener implements Listener {

    private final GamerManager gamerManager;

    @Inject
    public GamerStatListener(GamerManager gamerManager) {
        this.gamerManager = gamerManager;
    }

    @EventHandler
    public void onSettingsUpdated(SettingsUpdatedEvent event) {
        Optional<Gamer> gamerOptional = gamerManager.getObject(event.getClient().getUuid());
        gamerOptional.ifPresent(gamer -> {
            if(event.getSetting() instanceof GamerProperty key){
                gamerManager.getGamerRepository().saveProperty(gamer, key, gamer.getProperties().get(key));
            }
        });

        UtilServer.callEvent(new ScoreboardUpdateEvent(event.getPlayer()));
    }

    @UpdateEvent(delay = 120_000)
    public void processStatUpdates(){
        gamerManager.getGamerRepository().processStatUpdates(true);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(event.isCancelled()) return;

        Player player = event.getPlayer();

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        gamerOptional.ifPresent(gamer -> {
            int blocksPlaced = (int) (gamer.getProperty(GamerProperty.BLOCKS_PLACED).orElse(0)) + 1;
            gamer.putProperty(GamerProperty.BLOCKS_PLACED, blocksPlaced);
            UtilServer.callEvent(new SettingsUpdatedEvent(player, gamer.getClient(), GamerProperty.BLOCKS_PLACED));
        });
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockBroken(BlockBreakEvent event) {
        if(event.isCancelled()) return;
        System.out.println(UtilMapMaterial.getBlockColor(event.getBlock()).id);
        Player player = event.getPlayer();

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
        gamerOptional.ifPresent(gamer -> {
            int blocksBroken = (int) (gamer.getProperty(GamerProperty.BLOCKS_BROKEN).orElse(0)) + 1;
            gamer.putProperty(GamerProperty.BLOCKS_BROKEN, blocksBroken);
            UtilServer.callEvent(new SettingsUpdatedEvent(player, gamer.getClient(), GamerProperty.BLOCKS_BROKEN));
        });
    }


}
