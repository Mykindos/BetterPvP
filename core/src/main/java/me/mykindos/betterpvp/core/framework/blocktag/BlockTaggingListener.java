package me.mykindos.betterpvp.core.framework.blocktag;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@BPvPListener
@Singleton
@CustomLog
public class BlockTaggingListener implements Listener {

    private final Core core;
    private final ClientManager clientManager;
    private final BlockTagManager blockTagManager;

    @Inject
    public BlockTaggingListener(Core core, ClientManager clientManager, BlockTagManager blockTagManager) {
        this.core = core;
        this.clientManager = clientManager;
        this.blockTagManager = blockTagManager;
    }

    @EventHandler
    public void onAdminUnTag(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.STICK) return;

        Block block = event.getClickedBlock();
        Client client = clientManager.search().online(player);
        if (!client.isAdministrating()) return;

        untagBlock(block);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this::untagBlock);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        //untagBlock(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        tagBlock(event.getBlock(), event.getPlayer() != null ? event.getPlayer().getUniqueId() : null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockMultiPlace(BlockMultiPlaceEvent event) {
        event.getReplacedBlockStates().forEach(blockState -> tagBlock(blockState.getBlock(), event.getPlayer().getUniqueId()));
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
        tagBlock(event.getBlock(), event.getPlayer().getUniqueId());
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
        UUID player = blockTagManager.getPlayerPlaced(block);
        if (player == null) return;

        final Block relative = block.getRelative(direction);
        untagBlock(block);
        tagBlock(relative, player);
    }

    // Run next tick to allow other plugins to read the block
    private void tagBlock(Block block, @Nullable UUID uuid) {
        blockTagManager.addBlockTag(block, new BlockTag(BlockTags.PLAYER_MANIPULATED.getTag(), uuid != null ? uuid.toString() : null));
    }

    // Run next tick to allow other plugins to read the block
    private void untagBlock(Block block) {
        if (blockTagManager.isPlayerPlaced(block)) {
            UtilServer.runTaskLater(core, () -> {
                blockTagManager.removeBlockTag(block, BlockTags.PLAYER_MANIPULATED.getTag());
            }, 1L);

        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        BlockTagManager.BLOCKTAG_CACHE.invalidate(UtilWorld.chunkToFile(event.getChunk()));
        // We don't need to do this, but doesn't hurt to speed things up.
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        blockTagManager.loadChunkIfAbsent(event.getTo().getChunk());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        // TODO remove this later
        event.getChunk().getPersistentDataContainer().remove(CoreNamespaceKeys.BLOCK_TAG_CONTAINER_KEY);
    }

    @UpdateEvent(delay = 1000L * 60L * 60L * 5L)
    public void purgeBlockTags() {
        CompletableFuture.runAsync(() -> {
            blockTagManager.getBlockTagRepository().purgeOldBlockTags();
        }).exceptionally(ex -> {
            log.error("Failed to purge old block tags", ex).submit();
            return null;
        });
    }

    @UpdateEvent(delay = 30000)
    public void processBlockTagUpdates() {
        CompletableFuture.runAsync(() -> {
            blockTagManager.getBlockTagRepository().processBlockTagUpdates();
        }).exceptionally(ex -> {
            log.error("Failed to process block tag updates", ex).submit();
            return null;
        });
    }

}
