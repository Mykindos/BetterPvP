package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTagManager;
import me.mykindos.betterpvp.core.framework.blocktag.BlockTags;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

@BPvPListener
@Singleton
public class SaplingGrowListener implements Listener {

    private final BlockTagManager blockTagManager;

    @Inject
    public SaplingGrowListener(BlockTagManager blockTagManager) {
        this.blockTagManager = blockTagManager;
    }

    /**
     * This listener's purpose is to remove the player placed data on a sapling block when it grows into
     * a tree.
     */
    @EventHandler
    public void onSaplingGrow(StructureGrowEvent event) {
        if (event.isCancelled()) return;
        Block block = event.getLocation().getBlock();

        UtilServer.runTaskLater(JavaPlugin.getPlugin(Progression.class), () -> {
            blockTagManager.removeBlockTag(block, BlockTags.PLAYER_MANIPULATED.getTag());
        }, 1L);
    }
}
