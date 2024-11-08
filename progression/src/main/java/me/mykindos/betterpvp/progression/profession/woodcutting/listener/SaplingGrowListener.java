package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.progression.profession.woodcutting.WoodcuttingHandler;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.persistence.PersistentDataContainer;

@BPvPListener
@Singleton
public class SaplingGrowListener implements Listener {
    private final WoodcuttingHandler woodcuttingHandler;

    @Inject
    public SaplingGrowListener(WoodcuttingHandler woodcuttingHandler) {
        this.woodcuttingHandler = woodcuttingHandler;
    }

    /**
     * This listener's purpose is to remove the player placed data on a sapling block when it grows into
     * a tree.
     */
    @EventHandler
    public void onSaplingGrow(StructureGrowEvent event) {
        Block block = event.getLocation().getBlock();

        if (woodcuttingHandler.didPlayerPlaceBlock(block)) {
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
            pdc.remove(CoreNamespaceKeys.PLAYER_PLACED_KEY);
            UtilBlock.setPersistentDataContainer(block, pdc);
        }
    }
}
