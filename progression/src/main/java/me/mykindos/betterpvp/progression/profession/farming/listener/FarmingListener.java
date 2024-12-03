package me.mykindos.betterpvp.progression.profession.farming.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.farming.FarmingHandler;
import me.mykindos.betterpvp.progression.profession.farming.repository.FarmingActionType;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

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

    // This method will eventually need to fire a custom event for bonemeal for farming perks
    @EventHandler(priority = EventPriority.MONITOR)
    public void onBonemealCrop(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Material materialUsed = event.getMaterial();
        if (!materialUsed.equals(Material.BONE_MEAL)) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;
        if (!farmingHandler.getExperiencePerCropWhenHarvested().containsKey(clickedBlock.getType())) return;

        BlockData clickedBlockData = clickedBlock.getBlockData();
        if (!(clickedBlockData instanceof Ageable blockAsAgeable)) return;  // Probably unreachable return stmt but who knows

        // While you can click the block w/ bonemeal, we don't want to yield xp for it since crop is fully grown
        if (blockAsAgeable.getAge() == blockAsAgeable.getMaximumAge()) return;

        // I don't think get plugin is super expensive operation
        Progression progression = JavaPlugin.getPlugin(Progression.class);

        // This is removed when harvested (in farming Handler)
        if (!clickedBlock.hasMetadata(farmingHandler.LOW_YIELD_METADATA_KEY)) {
            clickedBlock.setMetadata(farmingHandler.LOW_YIELD_METADATA_KEY, new FixedMetadataValue(progression, true));
        }

        farmingHandler.attemptToHarvestCrop(event.getPlayer(), clickedBlock, FarmingActionType.BONEMEAL);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlantCropEvent(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block blockPlaced = event.getBlockPlaced();
        if (!farmingHandler.getExperiencePerCropWhenHarvested().containsKey(blockPlaced.getType())) return;

        // Sugar cane, mushrooms, etc. we don't want to grant xp for those because it gets weird with placing them
        if (!(blockPlaced.getBlockData() instanceof Ageable)) return;

        farmingHandler.attemptToHarvestCrop(event.getPlayer(), blockPlaced, FarmingActionType.PLANT);
    }

    /**
     * Whenever farmland is broken, the crop may still have low-yield metadata.
     * This event will take care of removing that.
     */
    @EventHandler
    public void onBreakFarmland(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block brokenBlock = event.getBlock();
        if (!brokenBlock.getType().equals(Material.FARMLAND)) return;

        Block cropAbove = brokenBlock.getRelative(0, 1, 0);
        if (cropAbove.hasMetadata(farmingHandler.LOW_YIELD_METADATA_KEY)) {
            cropAbove.removeMetadata(farmingHandler.LOW_YIELD_METADATA_KEY, JavaPlugin.getPlugin(Progression.class));
        }
    }
}
