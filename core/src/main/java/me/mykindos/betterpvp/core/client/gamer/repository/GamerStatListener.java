package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@BPvPListener
@CustomLog
public class GamerStatListener implements Listener {

    private final ClientManager clientManager;
    private final DamageLogManager damageLogManager;

    @Inject
    public GamerStatListener(ClientManager clientManager, DamageLogManager damageLogManager) {
        this.clientManager = clientManager;
        this.damageLogManager = damageLogManager;
    }

    @EventHandler
    public void onSettingsUpdated(GamerPropertyUpdateEvent event) {
        clientManager.saveGamerProperty(event.getContainer(), event.getProperty(), event.getNewValue());
    }
    //todo add migrations/one off stats for legacy period, all of these stats are currently done via Statistic (Minecraft)
/*
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

        int blocksBroken = (gamer.getIntProperty(GamerProperty.BLOCKS_BROKEN)) + 1;
        gamer.saveProperty(GamerProperty.BLOCKS_BROKEN, blocksBroken);
    }
 */

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        clientManager.getSqlLayer().processPropertyUpdates(event.getPlayer().getUniqueId(), true);
    }


}
