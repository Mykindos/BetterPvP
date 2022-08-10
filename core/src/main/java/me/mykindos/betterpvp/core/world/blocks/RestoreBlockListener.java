package me.mykindos.betterpvp.core.world.blocks;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ListIterator;

@BPvPListener
public class RestoreBlockListener implements Listener {

    private final WorldBlockHandler blockHandler;

    @Inject
    public RestoreBlockListener(WorldBlockHandler blockHandler) {
        this.blockHandler = blockHandler;
    }

    @EventHandler
    public void onBlock(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (blockHandler.isRestoreBlock(block)) {
            event.setCancelled(true);
        }

    }

    @UpdateEvent
    public void processBlocks() {
        ListIterator<RestoreBlock> restoreBlockListIterator = blockHandler.getRestoreBlocks().listIterator();
        while(restoreBlockListIterator.hasNext()){
            RestoreBlock next = restoreBlockListIterator.next();
            if(System.currentTimeMillis() >= next.getExpire()) {
                next.restore();
                restoreBlockListIterator.remove();
            }
        }

    }
}
