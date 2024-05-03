package me.mykindos.betterpvp.core.framework.blocktag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.UUID;

@BPvPListener
@Singleton
public class BlockTaggingListener implements Listener {

    @Inject
    private Core core;

    @Inject
    private ClientManager clientManager;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler   (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this::untagBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        tagBlock(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        event.getReplacedBlockStates().forEach(blockState -> tagBlock(blockState.getBlock(), event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(block -> shift(block, event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky()) return;
        event.getBlocks().forEach(block -> shift(block, event.getDirection()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        tagBlock(event.getBlock(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        tagBlock(event.getBlock(), player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPrime(TNTPrimeEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        event.getBlocks().forEach(state -> untagBlock(state.getBlock()));
    }

    private void shift(Block block, BlockFace direction) {
        final UUID player = getTaggedPlayer(block);
        if (player == null) return; // It wasn't a player placed block
        final Block relative = block.getRelative(direction);
        untagBlock(block);
        tagBlock(relative, player);
    }

    private UUID getTaggedPlayer(Block block) {
        final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
        return pdc.get(CoreNamespaceKeys.PLAYER_PLACED_KEY, CustomDataType.UUID);
    }

    private void tagBlock(Block block, Player player) {
        if (player == null || player.getGameMode() == GameMode.CREATIVE) return;
        if (clientManager.search().online(player).isAdministrating()) return;
        tagBlock(block, player.getUniqueId());
    }

    // Run next tick to allow other plugins to read the block
    private void tagBlock(Block block, UUID uuid) {
        UtilServer.runTaskLater(core, false, () -> {
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
            pdc.set(CoreNamespaceKeys.PLAYER_PLACED_KEY, CustomDataType.UUID, uuid);
            UtilBlock.setPersistentDataContainer(block, pdc);
        }, 1L);
    }

    // Run next tick to allow other plugins to read the block
    private void untagBlock(Block block) {
        UtilServer.runTaskLater(core, false, () -> {
            final PersistentDataContainer pdc = UtilBlock.getPersistentDataContainer(block);
            pdc.remove(CoreNamespaceKeys.PLAYER_PLACED_KEY);
            UtilBlock.setPersistentDataContainer(block, pdc);
        }, 1L);
    }

}
