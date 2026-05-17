package me.mykindos.betterpvp.progression.profession.mining.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@BPvPListener
@CustomLog
@Singleton
public class MiningListener implements Listener {

    private final ClientManager clientManager;
    private final MiningHandler miningHandler;

    @Inject
    public MiningListener(ClientManager clientManager, MiningHandler miningHandler) {
        this.clientManager = clientManager;
        this.miningHandler = miningHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Client client = clientManager.search().online(event.getPlayer());
        if (client.isAdministrating() || event.getPlayer().getGameMode().isInvulnerable()) {
            return;
        }

        final Material type = event.getBlock().getType();
        if (!UtilBlock.isOre(type) && !Tag.MINEABLE_PICKAXE.isTagged(type)) {
            return;
        }

        miningHandler.attemptMineOre(event.getPlayer(), event.getBlock());
    }
}
