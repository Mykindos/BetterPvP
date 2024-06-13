package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@BPvPListener
public class GamerStatListener implements Listener {

    private final ClientManager clientManager;

    @Inject
    public GamerStatListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onSettingsUpdated(GamerPropertyUpdateEvent event) {
        clientManager.saveGamerProperty(event.getGamer(), event.getProperty(), event.getValue());
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final Gamer gamer = client.getGamer();

        int blocksPlaced = (int) (gamer.getProperty(GamerProperty.BLOCKS_PLACED).orElse(0)) + 1;
        gamer.saveProperty(GamerProperty.BLOCKS_PLACED, blocksPlaced);
    }

    @EventHandler (priority = EventPriority.MONITOR)
    public void onBlockBroken(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        final Client client = clientManager.search().online(player);
        final Gamer gamer = client.getGamer();

        int blocksBroken = (int) (gamer.getProperty(GamerProperty.BLOCKS_BROKEN).orElse(0)) + 1;
        gamer.saveProperty(GamerProperty.BLOCKS_BROKEN, blocksBroken);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clientManager.getSqlLayer().processStatUpdates(event.getPlayer().getUniqueId(), true);
    }


}
