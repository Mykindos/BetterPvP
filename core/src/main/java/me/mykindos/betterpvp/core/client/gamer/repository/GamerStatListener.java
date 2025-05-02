package me.mykindos.betterpvp.core.client.gamer.repository;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerPropertyUpdateEvent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
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
        clientManager.saveGamerProperty(event.getContainer(), event.getProperty(), event.getNewValue());
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

        int blocksBroken = (gamer.getIntProperty(GamerProperty.BLOCKS_BROKEN)) + 1;
        gamer.saveProperty(GamerProperty.BLOCKS_BROKEN, blocksBroken);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(CustomDeathEvent event) {
        if (!(event.getKilled() instanceof Player player)) return;
        final Client client = clientManager.search().online(player);
        final Gamer gamer = client.getGamer();
        int deaths = gamer.getIntProperty(GamerProperty.DEATHS) + 1;
        gamer.saveProperty(GamerProperty.DEATHS, deaths);
    }

    //todo remove
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobKill(EntityDeathEvent event) {
        final LivingEntity killed = event.getEntity();
        if (killed instanceof Player) return;
        if (killed.getLastDamageCause() == null ||
                !(killed.getLastDamageCause().getDamageSource().getCausingEntity() instanceof Player player)) return;

        final Client client = clientManager.search().online(player);
        final Gamer gamer = client.getGamer();
        int mobsKilled = gamer.getIntProperty(GamerProperty.MOB_KILLS) + 1;
        gamer.saveProperty(GamerProperty.MOB_KILLS, mobsKilled);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        clientManager.getSqlLayer().processStatUpdates(event.getPlayer().getUniqueId(), true);
    }


}
