package me.mykindos.betterpvp.progression.profession.farming.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.profession.farming.FarmingHandler;
import me.mykindos.betterpvp.progression.profession.farming.repository.FarmingActionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@BPvPListener
@CustomLog
@Singleton
public class FarmingListener implements Listener {
    private final FarmingHandler farmingHandler;

    @Inject
    public FarmingListener(FarmingHandler farmingHandler) {
        this.farmingHandler = farmingHandler;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHarvestCrop(BlockBreakEvent event) {
        Block cropBlock = event.getBlock();
        if (!farmingHandler.getExperiencePerCropWhenHarvested().containsKey(cropBlock.getType())) return;

        farmingHandler.attemptToHarvestCrop(event.getPlayer(), cropBlock, FarmingActionType.HARVEST);
    }
}
