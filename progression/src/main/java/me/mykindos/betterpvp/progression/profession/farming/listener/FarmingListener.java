package me.mykindos.betterpvp.progression.profession.farming.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.farming.FarmingHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

// TODO: Add custom sweet berry bush code for harvesting it

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


        YieldLevel yieldLevel = YieldLevel.HIGH;
        boolean isCropSugarCane = cropBlock.getType().equals(Material.SUGAR_CANE);

        // Sugar Cane is Ageable but it doesn't work like other ageables
        if (cropBlock.getBlockData() instanceof Ageable cropAsAgeable && !isCropSugarCane) {

            // No matter what, the block is gone now so the low yield metadata must go away too
            if (cropBlock.hasMetadata(farmingHandler.LOW_YIELD_METADATA_KEY)) {
                cropBlock.removeMetadata(farmingHandler.LOW_YIELD_METADATA_KEY, JavaPlugin.getPlugin(Progression.class));

                // If the crop wasn't fully grown, then yieldLevel will get changed below but for now, set it to Low
                yieldLevel = YieldLevel.LOW;
            }

            boolean isCropFullyGrown = cropAsAgeable.getAge() == cropAsAgeable.getMaximumAge();

            // We don't want to reward players for harvesting early!
            if (!isCropFullyGrown) {
                yieldLevel = YieldLevel.NO_XP;
            }

            Progression progression = JavaPlugin.getPlugin(Progression.class);

            if (isCropFullyGrown && cropBlock.hasMetadata(farmingHandler.ALREADY_GAINED_XP_FROM_PLANTING_METADATA_KEY)) {
                cropBlock.removeMetadata(farmingHandler.ALREADY_GAINED_XP_FROM_PLANTING_METADATA_KEY, progression);
            }

        } else {
            if (farmingHandler.didPlayerPlaceBlock(cropBlock)) yieldLevel = YieldLevel.NO_XP;
        }

        farmingHandler.attemptToHarvestCrop(event.getPlayer(), cropBlock, yieldLevel, FarmingActionType.HARVEST);
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

        // Although sugar cane is ageable, bonemeal doesnt work on it
        if (clickedBlock.getType().equals(Material.SUGAR_CANE)) return;

        // While you can click the block w/ bonemeal, we don't want to yield xp for it since crop is fully grown
        if (blockAsAgeable.getAge() == blockAsAgeable.getMaximumAge()) return;

        // I don't think get plugin is super expensive operation
        Progression progression = JavaPlugin.getPlugin(Progression.class);

        // This is removed when harvested
        if (!clickedBlock.hasMetadata(farmingHandler.LOW_YIELD_METADATA_KEY)) {
            clickedBlock.setMetadata(farmingHandler.LOW_YIELD_METADATA_KEY, new FixedMetadataValue(progression, true));
        }

        farmingHandler.attemptToHarvestCrop(event.getPlayer(), clickedBlock, YieldLevel.LOW, FarmingActionType.BONEMEAL);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlantCropEvent(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        Block blockPlaced = event.getBlockPlaced();
        if (!farmingHandler.getExperiencePerCropWhenHarvested().containsKey(blockPlaced.getType())) return;

        // Sugar cane, mushrooms, etc. we don't want to grant xp for those because it gets weird with placing them
        if (!(blockPlaced.getBlockData() instanceof Ageable)) return;

        YieldLevel yieldLevel = YieldLevel.LOW;
        Player player = event.getPlayer();
        boolean isInvalidSugarCane = blockPlaced.getRelative(0, -1, 0).getType().equals(Material.SUGAR_CANE)
                && blockPlaced.getType().equals(Material.SUGAR_CANE);

        // If you place a piece of sugar cane on sugar cane, you shouldn't get exp
        if (isInvalidSugarCane) yieldLevel = YieldLevel.NO_XP;

        if (blockPlaced.hasMetadata(farmingHandler.ALREADY_GAINED_XP_FROM_PLANTING_METADATA_KEY)) {
            yieldLevel = YieldLevel.NO_XP;
            UtilMessage.simpleMessage(player, "Farming", "You must fully harvest a crop in order to gain experience from planting here");
        } else {
            if (!isInvalidSugarCane) {
                blockPlaced.setMetadata(farmingHandler.ALREADY_GAINED_XP_FROM_PLANTING_METADATA_KEY,
                        new FixedMetadataValue(JavaPlugin.getPlugin(Progression.class), true));
            }
        }


        farmingHandler.attemptToHarvestCrop(player, blockPlaced, yieldLevel, FarmingActionType.PLANT);
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
