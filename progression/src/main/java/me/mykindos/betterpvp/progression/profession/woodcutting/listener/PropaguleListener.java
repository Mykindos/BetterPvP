package me.mykindos.betterpvp.progression.profession.woodcutting.listener;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Handles Mangrove Propagule placing and interaction
 */
@BPvPListener
@Singleton
public class PropaguleListener implements Listener {

    /**
     * Listens for whenever a <b>Mangrove Propagule</b> is broken
     * <br>
     * Stops the item from dropping if the block <i>is not</i> player placed
     */
    @EventHandler
    public void onPropaguleBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        if (!block.getType().equals(Material.MANGROVE_PROPAGULE)) return;
        if (UtilBlock.getPersistentDataContainer(block).has(CoreNamespaceKeys.PLAYER_PLACED_KEY)) return;

        event.setDropItems(false);
    }

    @EventHandler
    public void preventMangroveLeavesDroppingItemsOnDecay(LeavesDecayEvent event) {
        if (event.isCancelled()) return;
        if (!event.getBlock().getType().equals(Material.MANGROVE_LEAVES)) return;

        event.getBlock().getDrops().clear();
    }

    @EventHandler
    public void preventMangroveLeavesMangroveLeavesDroppingItemsOnBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        if (!event.getBlock().getType().equals(Material.MANGROVE_LEAVES)) return;

        event.getBlock().getDrops().clear();
    }

    /**
     * Listens for when either type of mangrove tree grows
     * <br>
     * Sets all <b>Mangrove Propagules</b> to air
     */
    @EventHandler
    public void onMangroveTreeGrow(StructureGrowEvent event) {
        if (event.isCancelled()) return;
        if (!event.getSpecies().name().contains("MANGROVE")) return;
        List<BlockState> blockStates = event.getBlocks();

        for (BlockState state : blockStates) {
            if (state.getType().equals(Material.MANGROVE_PROPAGULE)) {
                state.setType(Material.AIR);
            }
        }
    }

    /**
     * Listens for & cancels when a player tries to bonemeal a mangrove leaf block
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBoneMealMangroveLeaves(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        Block block = event.getClickedBlock();
        if (block != null && item != null &&
                block.getType() == Material.MANGROVE_LEAVES &&
                item.getType() == Material.BONE_MEAL) {

            event.setCancelled(true);
            UtilMessage.simpleMessage(event.getPlayer(), "Progression", "You cannot use bone meal on mangrove leaves.");
        }
    }
}
